# Java Collections & Concurrency — Decision Guide
### FinTech context: payments, FX, accounts, notifications at 70M+ users

> **How to read this:** Start at the **problem** you're solving, not the data structure.
> Each node answers: *what to use → why it works → what breaks → how to fix it.*

---

## Master Decision Tree

```
What is your core problem?
│
├── I need to store and look up data concurrently
│   ├── Key → Value map ─────────────────────────────────────────► § 1.1  ConcurrentHashMap
│   ├── Ordered / sorted map ────────────────────────────────────► § 1.2  ConcurrentSkipListMap
│   ├── Unique elements / membership test ──────────────────────► § 1.3  ConcurrentSkipListSet
│   └── Simple list / snapshot ──────────────────────────────────► § 1.4  CopyOnWriteArrayList
│
├── I need a single number or reference to be updated atomically
│   ├── Counter (long / int) ────────────────────────────────────► § 2.1  AtomicLong / LongAdder
│   ├── Object reference ────────────────────────────────────────► § 2.2  AtomicReference
│   └── Compound field update (balance + version) ──────────────► § 2.3  AtomicStampedReference
│
├── I need threads to hand off work or pace each other
│   ├── Bounded buffer (backpressure) ───────────────────────────► § 3.1  ArrayBlockingQueue
│   ├── Unbounded buffer (drop-in speed) ───────────────────────► § 3.2  LinkedBlockingQueue
│   ├── Priority-ordered tasks ──────────────────────────────────► § 3.3  PriorityBlockingQueue
│   ├── Scheduled / delayed execution ──────────────────────────► § 3.4  DelayQueue
│   └── Zero-buffer rendezvous ──────────────────────────────────► § 3.5  SynchronousQueue
│
├── I need to guard a critical section
│   ├── Simple mutual exclusion ─────────────────────────────────► § 4.1  synchronized
│   ├── Timeout / interruptible / fair lock ────────────────────► § 4.2  ReentrantLock
│   ├── Many readers, rare writers ──────────────────────────────► § 4.3  ReentrantReadWriteLock
│   └── Optimistic reads, occasional writes ────────────────────► § 4.4  StampedLock
│
├── I need threads to coordinate a lifecycle event
│   ├── Wait for N tasks to finish ──────────────────────────────► § 5.1  CountDownLatch
│   ├── All threads reach a point before any continues ─────────► § 5.2  CyclicBarrier
│   ├── Limit concurrent access to a resource ──────────────────► § 5.3  Semaphore
│   └── One thread signals another of a condition ─────────────► § 5.4  Condition (with Lock)
│
└── I need to run tasks asynchronously / manage a pool
    ├── Fixed-size pool ─────────────────────────────────────────► § 6.1  FixedThreadPool
    ├── I/O-heavy work at scale (Java 21) ──────────────────────► § 6.2  VirtualThreadExecutor
    ├── Chained async pipelines ─────────────────────────────────► § 6.3  CompletableFuture
    └── CPU-bound divide-and-conquer ───────────────────────────► § 6.4  ForkJoinPool
```

---

---

## § 1 — Concurrent Collections

### § 1.1 `ConcurrentHashMap`

**Use when:** Multiple threads read and write a shared map simultaneously.

**FinTech examples:**
- In-memory idempotency key store: `key → PaymentResult`
- Per-user rate limit counters: `userId → WindowCounter`
- Account cache: `accountId → Account`

```java
ConcurrentHashMap<String, PaymentResult> idempotencyStore = new ConcurrentHashMap<>();

// Atomic "process only once" — safe even under concurrent retries
PaymentResult result = idempotencyStore.computeIfAbsent(
    request.idempotencyKey(),
    key -> paymentHandler.execute(request)
);
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| `computeIfAbsent` may invoke the function twice under high contention on the same key (JDK < 21 edge case) | Two threads can both enter the mapping function before the first completes | Add a per-key `ReentrantLock` as a secondary guard for slow mapping functions |
| Compound check-then-act is not atomic | `get` + `put` is two separate operations | Use `compute`, `merge`, or `putIfAbsent` — never `get` + `put` in separate calls |
| Full-map consistency (size, iteration) reflects a snapshot, not a live count | Segment-level locking, not global | Use `mappingCount()` instead of `size()`; accept that iteration may miss very recent entries |
| No blocking `get` — returns `null` if absent | By design | Use `computeIfAbsent` or a `BlockingQueue` if blocking is the intent |

---

### § 1.2 `ConcurrentSkipListMap`

**Use when:** You need a sorted, concurrent map — range queries or ordered iteration matter.

**FinTech examples:**
- Order book: bids and asks sorted by price
- Time-windowed transaction log sorted by `Instant`
- Sorted leaderboard of FX rates

```java
ConcurrentSkipListMap<Instant, Transaction> txLog = new ConcurrentSkipListMap<>();
txLog.put(Instant.now(), transaction);

// Range query: last 5 minutes of transactions — thread-safe
NavigableMap<Instant, Transaction> recent =
    txLog.tailMap(Instant.now().minus(5, ChronoUnit.MINUTES));
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Higher memory overhead than `ConcurrentHashMap` | Skip list maintains multiple pointer layers per node | Only use when sorted order or range queries are genuinely needed |
| Slower writes — O(log n) vs O(1) amortised | Probabilistic pointer structure must be maintained | Acceptable for order-book sizes; not for millions-per-second write paths |
| `size()` is O(n) | No cached counter due to concurrent modifications | Avoid `size()` in hot paths; maintain a separate `AtomicLong` counter if needed |

---

### § 1.3 `CopyOnWriteArrayList` / `CopyOnWriteArraySet`

**Use when:** Reads vastly outnumber writes; iteration must never throw `ConcurrentModificationException`.

**FinTech examples:**
- Observer list for account event listeners (rarely changes, iterated on every event)
- Static list of supported currencies
- In-memory domain event log drained by a background thread

```java
CopyOnWriteArrayList<AccountEventListener> listeners = new CopyOnWriteArrayList<>();

// Safe iteration — snapshot is taken at iterator creation
for (AccountEventListener listener : listeners) {
    listener.onEvent(event);   // no CME even if another thread adds a listener
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Every write copies the entire backing array | Immutable snapshot semantics | Only use when writes are rare (e.g. startup-time registration). Use `ConcurrentHashMap.newKeySet()` for write-heavy sets |
| Iterators see a stale snapshot | Copy is taken at iterator creation | Acceptable for observer patterns. If freshness is critical, use `ConcurrentLinkedQueue` instead |
| High GC pressure under frequent writes | Old array becomes garbage on every mutation | Switch to `ConcurrentLinkedDeque` or a `synchronized` `ArrayList` if writes are frequent |

---

---

## § 2 — Atomic Variables

### § 2.1 `AtomicLong` vs `LongAdder`

**Use when:** A counter is incremented/decremented by multiple threads without needing a lock.

**FinTech examples:**
- `AtomicLong`: account balance (read + CAS debit in a loop)
- `LongAdder`: metrics counter (total payments processed, total bytes transferred)

```java
// AtomicLong — CAS loop for balance (read the current value matters)
AtomicLong balance = new AtomicLong(10_000L);

boolean debit(long amount) {
    long current;
    do {
        current = balance.get();
        if (current < amount) return false;
    } while (!balance.compareAndSet(current, current - amount));
    return true;
}

// LongAdder — high-throughput counter (exact value only needed at read time)
LongAdder paymentsProcessed = new LongAdder();
paymentsProcessed.increment();          // contention-free increment
long total = paymentsProcessed.sum();   // read (slightly approximate under contention)
```

| | `AtomicLong` | `LongAdder` |
|---|---|---|
| Read current value at any time | ✅ Exact, always | ⚠️ `sum()` is approximate under concurrent increments |
| Write throughput under contention | ⚠️ CAS retries cause spinning | ✅ Striped cells eliminate contention |
| Memory | ✅ 1 long | ⚠️ Cell array (grows with contention) |
| Use for | Balance, version counters, idempotency keys | Metrics, rate counters, throughput gauges |

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| ABA problem in CAS loop | Thread A reads 100, B changes 100→50→100, A's CAS succeeds incorrectly | Use `AtomicStampedReference` (§ 2.3) which carries a version number alongside the value |
| Spinning under extreme contention | CAS fails repeatedly, wasting CPU | Fall back to `ReentrantLock` when contention is consistently high; or use `LongAdder` for pure counts |

---

### § 2.2 `AtomicReference`

**Use when:** You need to atomically swap an object reference (e.g. replace an entire snapshot).

**FinTech examples:**
- Hot-swap FX rate table without locking all readers
- Atomic configuration reload
- Replace a cache snapshot atomically

```java
AtomicReference<Map<String, BigDecimal>> fxRates = new AtomicReference<>(loadInitialRates());

// Writer: replace the entire map atomically — readers always see a consistent snapshot
void refreshRates(Map<String, BigDecimal> newRates) {
    fxRates.set(Collections.unmodifiableMap(newRates));
}

// Reader: get a reference to the current snapshot — no lock needed
BigDecimal rate = fxRates.get().get("GBPEUR");
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Entire object replaced on every update | Swap semantics — you replace the reference, not mutate the object | Fine for read-heavy cases (FX rates). For partial updates, use `ConcurrentHashMap` instead |
| Readers may see the old snapshot until the next `get()` | JMM visibility — `AtomicReference` is `volatile` so propagation is fast but not instantaneous | For hard real-time requirements, add a `ReadWriteLock` around the swap |

---

### § 2.3 `AtomicStampedReference` / `AtomicMarkableReference`

**Use when:** CAS on a reference needs a version number to detect the ABA problem.

**FinTech example:** Optimistic locking on a domain object without a database round-trip.

```java
AtomicStampedReference<AccountState> state =
    new AtomicStampedReference<>(initialState, 0);

void updateBalance(long newBalance) {
    int[] stampHolder = new int[1];
    AccountState current = state.get(stampHolder);
    int currentStamp = stampHolder[0];

    AccountState updated = current.withBalance(newBalance);
    boolean success = state.compareAndSet(current, updated, currentStamp, currentStamp + 1);
    // If false → another thread modified state; caller should retry
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Verbose API compared to `AtomicLong` | Must carry and check the stamp on every operation | Wrap in a helper class. For DB-backed entities, prefer a `version` column with `UPDATE ... WHERE version = :v` |
| Stamp overflow (wraps at `Integer.MAX_VALUE`) | Integer stamp is finite | Negligible in practice for payment systems; use a monotonic `long` timestamp as stamp if needed |

---

---

## § 3 — Blocking Queues (Producer-Consumer)

### § 3.1 `ArrayBlockingQueue` — bounded, backpressure

**Use when:** You need to limit the amount of work in flight and push back on producers when the system is overwhelmed.

**FinTech example:** Notification dispatcher that must not accumulate unbounded memory when the push service is slow.

```java
BlockingQueue<PaymentEvent> queue = new ArrayBlockingQueue<>(1000); // hard cap

// Producer blocks when full — natural backpressure to the payment pipeline
void dispatch(PaymentEvent event) throws InterruptedException {
    queue.put(event);   // blocks if queue is full
}

// Consumer
void processLoop() {
    while (!Thread.interrupted()) {
        PaymentEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
        if (event != null) notificationService.send(event);
    }
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Producer blocks when full, causing upstream stalls | Bounded by design | Use `offer(e, timeout)` and reject/log if timeout exceeded; route to a dead-letter queue |
| Fixed capacity must be tuned carefully | Too small → frequent backpressure. Too large → OOM under sustained overload | Set capacity to `workerCount × expected_processing_time_ms × safe_multiplier`; expose queue depth as a metric |
| Single lock for head and tail | `ArrayBlockingQueue` uses one `ReentrantLock` | Switch to `LinkedBlockingQueue` (two locks) for higher producer/consumer independence at the cost of heap allocation |

---

### § 3.2 `LinkedBlockingQueue` — optionally bounded, two locks

**Use when:** Producer and consumer rates differ significantly and you want them to not contend on the same lock.

**FinTech example:** High-throughput event bus where producers (payment processors) and consumers (audit writers) operate at different rates.

```java
// Optionally bounded — always specify capacity in production to avoid OOM
BlockingQueue<AuditEvent> auditQueue = new LinkedBlockingQueue<>(50_000);
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Unbounded by default → OOM risk | `new LinkedBlockingQueue<>()` with no argument is unlimited | **Always** pass a capacity. Set an alert on `queue.size() > 0.8 * capacity` |
| Node allocation per element → GC pressure | Linked list nodes are heap objects | Use `ArrayBlockingQueue` for predictable, low-GC environments (e.g. payment hot path) |

---

### § 3.3 `PriorityBlockingQueue`

**Use when:** Some tasks must be processed before others regardless of arrival order.

**FinTech example:** Payment processing where SEPA Instant (real-time) payments have higher priority than standard BACS transfers.

```java
PriorityBlockingQueue<PaymentTask> queue = new PriorityBlockingQueue<>();

record PaymentTask(Payment payment, int priority) implements Comparable<PaymentTask> {
    @Override public int compareTo(PaymentTask other) {
        return Integer.compare(other.priority(), this.priority()); // higher priority first
    }
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Unbounded — no backpressure | Priority heap grows without limit | Wrap with a size check before `offer`; reject or escalate when over threshold |
| Low-priority tasks can starve indefinitely | High-priority tasks always jump the queue | Add an aging mechanism: increment priority of tasks waiting longer than a threshold |
| No guaranteed FIFO within same priority | Heap ordering, not insertion order | Add a sequence number as a tiebreaker in `compareTo` |

---

### § 3.4 `DelayQueue`

**Use when:** Tasks should be processed only after a delay has expired.

**FinTech example:** Scheduled payment retry after 30 seconds; token bucket refill timer; session expiry cleanup.

```java
DelayQueue<ScheduledRetry> retryQueue = new DelayQueue<>();

record ScheduledRetry(PaymentRequest request, Instant executeAt) implements Delayed {
    @Override public long getDelay(TimeUnit unit) {
        return unit.convert(Duration.between(Instant.now(), executeAt));
    }
    @Override public int compareTo(Delayed other) {
        return Long.compare(getDelay(NANOSECONDS), other.getDelay(NANOSECONDS));
    }
}

// Worker blocks until the soonest task is ready
ScheduledRetry retry = retryQueue.take(); // blocks until executeAt is reached
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Single JVM — retries lost on crash | In-memory only | Persist scheduled retries to a DB or Kafka with a scheduled topic; `DelayQueue` is only for ephemeral, best-effort scheduling |
| Heap grows unbounded | All future retries sit in memory | Cap the queue; for long-range scheduling use a persistent scheduler (Quartz, DB-backed cron) |

---

---

## § 4 — Locks and Mutual Exclusion

### § 4.1 `synchronized`

**Use when:** You need the simplest, lowest-ceremony mutual exclusion on a method or block.

```java
public synchronized boolean debit(long amount) {
    if (balance < amount) throw new InsufficientFundsException(...);
    balance -= amount;
    return true;
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Cannot interrupt a waiting thread | JVM built-in monitor — no hook for interruption | Use `ReentrantLock.lockInterruptibly()` (§ 4.2) |
| Cannot try with timeout | Binary lock/unlock only | Use `ReentrantLock.tryLock(timeout)` |
| Pins carrier thread in Java 21 virtual threads | JVM scheduler cannot unmount a virtual thread blocked on `synchronized` | Replace with `ReentrantLock` when mixing with virtual threads |
| No condition variables | `wait()`/`notifyAll()` is error-prone | Use `ReentrantLock` + `Condition` (§ 5.4) for explicit signal/await semantics |

---

### § 4.2 `ReentrantLock`

**Use when:** You need timeout, interruptibility, fairness, or multiple condition variables.

**FinTech example:** Transfer service acquiring two account locks with deadlock prevention via `tryLock`.

```java
ReentrantLock lock = new ReentrantLock(true); // fair: threads served in arrival order

// Deadlock-safe transfer: try both locks, back off if either times out
boolean transfer(Account from, Account to, long amount) throws InterruptedException {
    if (!from.getLock().tryLock(50, MILLISECONDS)) return false;
    try {
        if (!to.getLock().tryLock(50, MILLISECONDS)) return false;
        try {
            from.debit(amount);
            to.credit(amount);
            return true;
        } finally { to.getLock().unlock(); }
    } finally { from.getLock().unlock(); }
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Must manually unlock in `finally` | No automatic release like `synchronized` | Always wrap in try/finally; use try-with-resources via a `Lockable` wrapper |
| Fair mode reduces throughput | Fair queuing adds overhead on every lock | Only use fair mode when starvation is a real concern (e.g. many low-priority threads) |
| More verbose than `synchronized` | Explicit API | Acceptable — verbosity signals intent to reviewers |

---

### § 4.3 `ReentrantReadWriteLock`

**Use when:** Many threads read, few threads write, and reads don't need to be mutually exclusive.

**FinTech example:** FX rate cache: hundreds of payment threads read rates; one background thread refreshes them every 30 seconds.

```java
ReadWriteLock rwLock = new ReentrantReadWriteLock();

Map<String, BigDecimal> rates = new HashMap<>();

BigDecimal getRate(String pair) {
    rwLock.readLock().lock();
    try { return rates.get(pair); }
    finally { rwLock.readLock().unlock(); }
}

void refreshRates(Map<String, BigDecimal> newRates) {
    rwLock.writeLock().lock();
    try { rates.clear(); rates.putAll(newRates); }
    finally { rwLock.writeLock().unlock(); }
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Write starvation if reads are constant | Read lock is always held; writers starve | Use fair `ReentrantReadWriteLock(true)` or set a write-preference policy |
| Cannot upgrade read lock to write lock | Upgrading would require releasing the read lock first, opening a race | Explicitly release read lock, then acquire write lock; re-validate state after upgrade |
| More complex than `synchronized` | Two separate lock objects | Encapsulate behind a service interface; callers don't see the lock |
| Slower than `StampedLock` for read-heavy paths | Overhead of tracking active readers | Upgrade to `StampedLock` (§ 4.4) for maximum read throughput |

---

### § 4.4 `StampedLock`

**Use when:** Reads dominate overwhelmingly and you want optimistic (lock-free) read attempts.

**FinTech example:** Real-time FX rate lookup on every payment — reads happen millions of times per second, writes a few times per minute.

```java
StampedLock sl = new StampedLock();
double fxRate;

double readRate() {
    long stamp = sl.tryOptimisticRead();       // no lock acquired
    double value = fxRate;
    if (!sl.validate(stamp)) {                 // check if a write happened
        stamp = sl.readLock();                 // fall back to pessimistic read
        try { value = fxRate; }
        finally { sl.unlockRead(stamp); }
    }
    return value;
}

void updateRate(double newRate) {
    long stamp = sl.writeLock();
    try { fxRate = newRate; }
    finally { sl.unlockWrite(stamp); }
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Not reentrant | Design constraint — no owner tracking | Never call another method that also acquires the same `StampedLock` |
| Cannot interrupt a waiting writer | Simpler than `ReentrantLock` | Use a `tryWriteLock(timeout)` variant and handle failure explicitly |
| Optimistic read can loop if writes are frequent | `validate()` fails → falls back to pessimistic read | Only worth it when writes are genuinely rare; otherwise use `ReadWriteLock` |
| Complex API — easy to misuse stamp | Stamps must be used correctly or the lock is corrupted | Encapsulate entirely in a helper class; never expose the stamp to callers |

---

---

## § 5 — Thread Coordination

### § 5.1 `CountDownLatch`

**Use when:** One or more threads must wait until a fixed number of operations complete.

**FinTech example:** Integration test — wait for all Kafka consumers to process a batch before asserting results.

```java
CountDownLatch allProcessed = new CountDownLatch(100);

for (int i = 0; i < 100; i++) {
    executor.submit(() -> {
        try { processPayment(queue.take()); }
        finally { allProcessed.countDown(); }
    });
}

boolean completed = allProcessed.await(10, TimeUnit.SECONDS);
assertThat(completed).isTrue();
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Single-use — cannot be reset | Count goes to zero and stays | Use `CyclicBarrier` (§ 5.2) if you need to reuse, or create a new latch per batch |
| No way to cancel or abort | All threads must count down | Set a timeout on `await()` and treat timeout as partial failure |

---

### § 5.2 `CyclicBarrier`

**Use when:** A fixed group of threads must all reach a checkpoint before any proceeds.

**FinTech example:** Parallel FX rate fetchers from multiple providers — wait for all to respond before choosing the best rate.

```java
CyclicBarrier barrier = new CyclicBarrier(3, this::chooseBestRate); // 3 providers

void fetchFromProvider(FxProvider provider) {
    BigDecimal rate = provider.fetch("GBPEUR");
    ratesCollected.put(provider.name(), rate);
    barrier.await(); // all three threads reach here before chooseBestRate runs
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| One broken thread breaks all | If any thread throws, `BrokenBarrierException` is thrown to all | Handle exceptions; reset with `barrier.reset()` but only after all threads have exited the barrier |
| All parties must arrive — no timeout per party | The slowest provider delays everyone | Use `barrier.await(timeout, unit)` per thread; treat timeout as a provider failure and use a fallback rate |

---

### § 5.3 `Semaphore`

**Use when:** You need to limit the number of threads concurrently accessing a resource.

**FinTech example:** Limit concurrent outbound calls to a third-party KYC API that has a rate limit of 10 concurrent connections.

```java
Semaphore kycSlots = new Semaphore(10); // max 10 concurrent KYC calls

KycResult verify(UserId userId) throws InterruptedException {
    kycSlots.acquire();
    try {
        return kycProvider.verify(userId);
    } finally {
        kycSlots.release();
    }
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| No ownership — any thread can release | Semaphore is not tied to the acquiring thread | Enforce discipline via try/finally; never release without acquiring |
| Fairness off by default — starvation possible | Non-fair mode allows barging | Use `new Semaphore(n, true)` for fair queuing |
| Does not prevent callers from queuing indefinitely | `acquire()` blocks without limit | Use `tryAcquire(timeout)` and fail-fast with a 503 when the system is overloaded (circuit breaker pattern) |

---

### § 5.4 `Condition` (with `ReentrantLock`)

**Use when:** Threads need to wait for a specific state change and be signalled precisely.

**FinTech example:** A balance monitor that alerts as soon as a balance drops below a threshold.

```java
ReentrantLock lock       = new ReentrantLock();
Condition     belowLimit = lock.newCondition();

// Producer — debits balance
void debit(long amount) {
    lock.lock();
    try {
        balance -= amount;
        if (balance < ALERT_THRESHOLD) belowLimit.signalAll();
    } finally { lock.unlock(); }
}

// Consumer — waits for low-balance condition
void awaitLowBalance() throws InterruptedException {
    lock.lock();
    try {
        while (balance >= ALERT_THRESHOLD) belowLimit.await(); // atomically releases lock + waits
        triggerAlert(balance);
    } finally { lock.unlock(); }
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Spurious wakeups must be handled | JVM/OS may wake threads without `signal()` | Always re-check the condition in a `while` loop, never `if` |
| More complex than `Object.wait/notify` | Explicit lock ownership required | The verbosity is the point — it's clearer and less error-prone than `wait`/`notifyAll` |

---

---

## § 6 — Executors and Async Patterns

### § 6.1 `Executors.newFixedThreadPool`

**Use when:** CPU-bound work or a predictable number of concurrent tasks.

**FinTech example:** Payment batch processor with a known parallelism budget.

```java
ExecutorService pool = Executors.newFixedThreadPool(
    Runtime.getRuntime().availableProcessors() // CPU-bound: one thread per core
);
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Fixed size → idle threads waste memory or busy threads queue work | Can't adapt to load spikes | Use `ThreadPoolExecutor` with a bounded queue and a `CallerRunsPolicy` for organic backpressure |
| Uncaught exceptions in tasks silently swallow | `submit()` wraps exceptions in the `Future` | Always call `future.get()` or use a custom `ThreadFactory` that logs `UncaughtExceptionHandler` |
| No built-in timeout per task | Tasks can run forever | Use `future.get(timeout, unit)` and `future.cancel(true)` on timeout |

---

### § 6.2 `Executors.newVirtualThreadPerTaskExecutor` (Java 21)

**Use when:** I/O-bound tasks at scale — HTTP calls, DB queries, Kafka I/O.

**FinTech example:** Processing 100,000 concurrent notification sends or outbound API calls.

```java
try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
    payments.forEach(p -> executor.submit(() -> processPayment(p)));
} // auto-closes, waits for all tasks
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| `synchronized` blocks pin the carrier thread | JVM cannot unmount a virtual thread blocked inside a `synchronized` block | Replace `synchronized` with `ReentrantLock` in any code path that may run in a virtual thread |
| ThreadLocal can accumulate per-virtual-thread | Millions of virtual threads × ThreadLocal size = OOM | Prefer `ScopedValue` (Java 21 preview) over `ThreadLocal`; keep ThreadLocal values small |
| Not a silver bullet for CPU-bound work | Virtual threads park on I/O but still occupy the carrier for CPU work | Use a fixed-size platform thread pool for CPU-heavy computation; virtual threads for I/O |
| Downstream bottleneck shifts to the resource | Virtual threads can now saturate a DB connection pool instantly | Pair with a bounded connection pool; use `Semaphore` or a pool like HikariCP with a capped size |

---

### § 6.3 `CompletableFuture`

**Use when:** You need to chain, combine, or transform asynchronous results without blocking.

**FinTech example:** Run fraud check and FX rate lookup in parallel, then combine to complete a payment.

```java
CompletableFuture<FraudResult> fraudCheck =
    CompletableFuture.supplyAsync(() -> fraudService.check(payment), executor);

CompletableFuture<BigDecimal> fxRate =
    CompletableFuture.supplyAsync(() -> fxService.getRate(payment.currency()), executor);

CompletableFuture<PaymentResult> result = fraudCheck.thenCombine(fxRate,
    (fraud, rate) -> {
        if (fraud.isRejected()) return PaymentResult.rejected(fraud.reason());
        return ledger.post(payment, rate);
    });

result
    .orTimeout(2, TimeUnit.SECONDS)           // hard deadline
    .exceptionally(ex -> PaymentResult.failed(ex.getMessage()))
    .thenAccept(notificationService::notify);
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Exceptions wrap in `CompletionException` | Async boundary wraps the original cause | Always call `.getCause()` on caught `CompletionException`; use `.exceptionally()` at each stage |
| Uses `ForkJoinPool.commonPool` by default | Convenience default — shared across the JVM | Always pass a dedicated `executor` to `supplyAsync`/`thenApplyAsync` in production |
| Stack traces are unhelpful in async chains | Each stage runs on a different thread | Add context (payment ID, trace ID) to log in each `.thenApply()` stage; use OpenTelemetry context propagation |
| Complex chains are hard to reason about | Long method chains obscure the flow | For very complex orchestration, consider a Saga orchestrator or a library like Resilience4j |

---

### § 6.4 `ForkJoinPool`

**Use when:** CPU-bound divide-and-conquer — split a large problem, solve recursively, combine results.

**FinTech example:** Parallel aggregation of millions of transactions for a monthly statement; risk scoring across a large portfolio.

```java
class TransactionSumTask extends RecursiveTask<Long> {
    private static final int THRESHOLD = 1000;
    private final List<Transaction> transactions;

    @Override protected Long compute() {
        if (transactions.size() <= THRESHOLD) {
            return transactions.stream().mapToLong(Transaction::amount).sum();
        }
        int mid = transactions.size() / 2;
        TransactionSumTask left  = new TransactionSumTask(transactions.subList(0, mid));
        TransactionSumTask right = new TransactionSumTask(transactions.subList(mid, transactions.size()));
        left.fork();
        return right.compute() + left.join();
    }
}

Long total = ForkJoinPool.commonPool().invoke(new TransactionSumTask(transactions));
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Work-stealing adds overhead for small tasks | Each split and steal has a cost | Set a `THRESHOLD` — don't fork tasks smaller than a few hundred operations |
| `commonPool` is shared globally | Any heavy task can starve other users of the pool | Use `new ForkJoinPool(parallelism)` for large batch jobs; shut it down after use |
| Not suited for I/O-bound work | Worker threads block waiting for I/O, starving other tasks | Use virtual threads (§ 6.2) or `CompletableFuture` with a separate I/O executor for I/O work |

---

---

## Master Trade-off Summary

| Problem | First choice | Upgrade to | When to upgrade |
|---|---|---|---|
| Shared map, concurrent R/W | `ConcurrentHashMap` | `ConcurrentSkipListMap` | When sorted order or range queries are needed |
| Atomic counter | `AtomicLong` | `LongAdder` | When write contention is very high and exact real-time reads are not needed |
| Mutual exclusion | `synchronized` | `ReentrantLock` | When you need timeout, interruptibility, conditions, or virtual thread compatibility |
| Read-heavy shared state | `ReentrantReadWriteLock` | `StampedLock` | When reads dominate and you need optimistic (lock-free) read performance |
| Bounded work queue | `ArrayBlockingQueue` | `LinkedBlockingQueue` | When producers and consumers need independent locks to reduce contention |
| Thread-safe observer list | `CopyOnWriteArrayList` | `ConcurrentLinkedQueue` | When writes become frequent |
| Thread pool | `FixedThreadPool` | `VirtualThreadExecutor` | When tasks are I/O-bound and you need to scale beyond ~1k concurrent tasks |
| Async chaining | `CompletableFuture` | Dedicated Saga orchestrator | When the pipeline has many error paths, compensating actions, or cross-service transactions |
| Semaphore / rate limit | JVM `Semaphore` | Redis `INCR` + TTL | When the limiter must span multiple service instances |

---

*Designed for FinTech Round 2 — Java 21, PostgreSQL, Redis, Kubernetes context.*

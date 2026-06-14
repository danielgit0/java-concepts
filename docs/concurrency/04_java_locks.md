# Java Locks — Decision Guide
### FinTech context: payments, FX, accounts, notifications at 70M+ users

> **How to read this:** Start at the **problem** you're trying to solve.
> Each node answers: *what to use → why it works → what breaks → how to fix it.*
> Locks are not the first answer — always ask if you can avoid them first.

---

## The First Question: Do You Even Need a Lock?

```
Do you need shared mutable state?
│
├── NO — Can you make it immutable or thread-confined?
│   ├── Make the object a record / final fields ──────────────► No lock needed. Publish safely via volatile or AtomicReference.
│   ├── Confine to one thread (ThreadLocal) ─────────────────► No lock needed. Each thread has its own copy.
│   └── Use a lock-free structure (ConcurrentHashMap, Atomic*) ► No lock needed. See Collections guide.
│
└── YES — You have shared mutable state. Continue below.
    │
    ├── Single variable (counter, flag, reference)?
    │   └── ─────────────────────────────────────────────────► § 0  volatile / Atomic* (not a lock — but often enough)
    │
    ├── Need to protect a block of code (critical section)?
    │   ├── Simple, no timeout, no virtual threads ──────────► § 1  synchronized
    │   ├── Need timeout / interruptible / conditions ────────► § 2  ReentrantLock
    │   ├── Many readers, rare writers ───────────────────────► § 3  ReentrantReadWriteLock
    │   └── Reads dominate, writes are very rare ─────────────► § 4  StampedLock
    │
    ├── Need to coordinate two objects without deadlock?
    │   └── ─────────────────────────────────────────────────► § 5  Lock ordering / tryLock patterns
    │
    ├── Need threads to wait for a specific condition?
    │   ├── With synchronized ────────────────────────────────► § 6  wait / notifyAll
    │   └── With ReentrantLock ───────────────────────────────► § 7  Condition (preferred)
    │
    └── Need low-level thread parking / custom synchroniser?
        └── ─────────────────────────────────────────────────► § 8  LockSupport
```

---

---

## § 0 — `volatile` and `Atomic*` — Before You Reach for a Lock

**The rule:** if you only need *visibility* (one variable, one writer) or *single-operation atomicity* (increment, swap), you don't need a lock at all.

### `volatile` — visibility only, zero atomicity

```java
// Correct: one writer, many readers, single read/write (not compound)
private volatile boolean shutdownRequested = false;

// Writer (shutdown hook thread)
void requestShutdown() { shutdownRequested = true; }

// Reader (worker thread)
void processLoop() {
    while (!shutdownRequested) { processNext(); }
}
```

```java
// WRONG — volatile does not make compound actions atomic
// Both threads can read false, both set to true — double-initialisation
if (!initialised) {          // read
    initialised = true;      // write — NOT atomic together
    doSetup();
}
// Fix: use synchronized, or double-checked locking with volatile + synchronized (see § 1)
```

| Guarantee | `volatile` gives you | `volatile` does NOT give you |
|---|---|---|
| Visibility | ✅ Write is immediately visible to all threads | — |
| Atomicity | ✅ For single reads and single writes of `long`/`double` | ❌ Not for `i++`, check-then-act, or compound updates |
| Ordering | ✅ Happens-before between write and subsequent reads | ❌ Does not prevent reordering of surrounding instructions |

### `AtomicLong` / `AtomicReference` — single-variable atomicity

```java
// Correct: CAS loop — lock-free and atomic
private final AtomicLong balance = new AtomicLong(10_000L);

boolean debit(long amount) {
    long current;
    do {
        current = balance.get();
        if (current < amount) return false;
    } while (!balance.compareAndSet(current, current - amount));
    return true;
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Only one variable at a time | CAS operates on a single memory word | For compound state (balance + version), use `AtomicStampedReference` or a full lock |
| CAS spinning under high contention | Failed CAS retries in a loop burning CPU | Under sustained high contention, `ReentrantLock` or `LongAdder` beats CAS |
| ABA problem | Thread sees value A, another changes A→B→A, CAS incorrectly succeeds | Use `AtomicStampedReference<T>` — carries a version stamp alongside the value |

---

---

## § 1 — `synchronized` — The Default Lock

**Use when:** You need mutual exclusion, the code is straightforward, you don't need timeout or interruption, and you are not using Java 21 virtual threads in the same code path.

### Intrinsic lock on an instance

```java
public class Account {
    private long balance;

    // Lock is the Account instance itself
    public synchronized void debit(long amount) {
        if (balance < amount) throw new InsufficientFundsException();
        balance -= amount;
    }

    public synchronized void credit(long amount) {
        balance += amount;
    }

    public synchronized long getBalance() { return balance; }
}
```

### Intrinsic lock on a block (narrower scope — better)

```java
private final Object lock = new Object(); // explicit private lock object

public void processPayment(Payment payment) {
    validate(payment);                    // outside lock — pure computation

    synchronized (lock) {
        account.debit(payment.amount());  // inside lock — only what needs protection
    }

    notify(payment);                      // outside lock — I/O after release
}
```

### Double-checked locking (safe with `volatile`)

```java
// Correct lazy singleton — the only safe pattern without a framework
private volatile FxRateCache instance;

FxRateCache getInstance() {
    if (instance == null) {              // first check — no lock (fast path)
        synchronized (this) {
            if (instance == null) {      // second check — under lock
                instance = new FxRateCache();
            }
        }
    }
    return instance;
}
// volatile ensures the write to `instance` is visible before the reference is published
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| No timeout on lock acquisition | Threads wait indefinitely | Use `ReentrantLock.tryLock(timeout)` (§ 2) |
| Cannot interrupt a waiting thread | JVM monitor has no interruption hook | Use `ReentrantLock.lockInterruptibly()` (§ 2) |
| Pins carrier thread in Java 21 virtual threads | JVM cannot unmount a virtual thread blocked on a monitor | Replace with `ReentrantLock` wherever virtual threads may call the code |
| Coarse locking kills parallelism | One lock serialises all threads | Narrow the `synchronized` block to only the critical state; move I/O and computation outside |
| `this` as lock is leakable | External code can `synchronized(myObject)` and block your methods | Use a private `final Object lock = new Object()` so no external code can interfere |
| `wait()` / `notifyAll()` is error-prone | Must hold the lock, handle spurious wakeups, use `while` not `if` | Prefer `ReentrantLock` + `Condition` (§ 7) |

---

---

## § 2 — `ReentrantLock` — The Flexible Lock

**Use when:** You need any of: timeout, interruptibility, fair ordering, multiple condition variables, or virtual thread compatibility.

### Basic pattern — always unlock in `finally`

```java
private final ReentrantLock lock = new ReentrantLock();

public boolean debit(long amount) {
    lock.lock();
    try {
        if (balance < amount) return false;
        balance -= amount;
        return true;
    } finally {
        lock.unlock(); // MUST be in finally — never skip
    }
}
```

### `tryLock` with timeout — deadlock prevention

```java
// Transfer between two accounts without deadlock
// (see also § 5 for lock ordering approach)
public boolean transfer(Account from, Account to, long amount)
        throws InterruptedException {

    boolean fromLocked = false, toLocked = false;
    try {
        fromLocked = from.getLock().tryLock(50, MILLISECONDS);
        toLocked   = to.getLock().tryLock(50, MILLISECONDS);

        if (!fromLocked || !toLocked) return false; // back off, caller retries

        from.debit(amount);
        to.credit(amount);
        return true;
    } finally {
        if (toLocked)   to.getLock().unlock();
        if (fromLocked) from.getLock().unlock();
    }
}
```

### `lockInterruptibly` — cooperative cancellation

```java
public void processWithCancellation() throws InterruptedException {
    lock.lockInterruptibly(); // throws if thread is interrupted while waiting
    try {
        doWork();
    } finally {
        lock.unlock();
    }
}
// Caller can cancel via: workerThread.interrupt()
// Unlike synchronized, the waiting thread wakes up and unwinds cleanly
```

### Fair lock — FIFO ordering

```java
// Threads acquire in arrival order — prevents starvation
ReentrantLock fairLock = new ReentrantLock(true);

// Use when: long-running tasks, mixed priorities, starvation is observable
// Avoid when: maximum throughput matters — fair mode adds scheduling overhead
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Must manually `unlock()` in `finally` | No automatic release mechanism | Always write the `try { ... } finally { lock.unlock(); }` pattern immediately. Never skip. |
| Reentrant behaviour can mask bugs | A thread can acquire its own lock N times; must release N times | Count acquisitions in code review; reentrancy is rarely needed and usually signals a design issue |
| Fair mode reduces throughput significantly | Strict FIFO requires checking and maintaining a queue on every lock | Only enable fairness when starvation has been observed in production, not preemptively |
| More verbose than `synchronized` | Explicit API | Acceptable — the verbosity signals intent and forces you to think about the unlock path |
| Not compatible with `try-with-resources` directly | `Lock` doesn't implement `AutoCloseable` | Wrap: `class LockScope implements AutoCloseable { ... }` or use Guava's `Striped` |

---

---

## § 3 — `ReentrantReadWriteLock` — Split Read and Write Paths

**Use when:** Multiple threads read frequently, but writes are rare — and readers don't need to be mutually exclusive with each other.

```
Read lock:  Many threads can hold it simultaneously
Write lock: Exclusive — no readers or other writers allowed
```

### FX rate cache — canonical read-heavy example

```java
public class FxRateCache {

    private final ReentrantReadWriteLock rwLock = new ReentrantReadWriteLock();
    private final Lock readLock  = rwLock.readLock();
    private final Lock writeLock = rwLock.writeLock();

    private Map<String, BigDecimal> rates = new HashMap<>();

    // Called millions of times per second from payment threads
    public BigDecimal getRate(String currencyPair) {
        readLock.lock();
        try {
            return rates.getOrDefault(currencyPair, BigDecimal.ZERO);
        } finally {
            readLock.unlock();
        }
    }

    // Called once every 30 seconds by a background thread
    public void refresh(Map<String, BigDecimal> newRates) {
        writeLock.lock();
        try {
            rates = Map.copyOf(newRates);
        } finally {
            writeLock.unlock();
        }
    }
}
```

### Lock upgrade — NOT supported (this is a trap)

```java
// WRONG — you cannot upgrade a read lock to a write lock
readLock.lock();
try {
    if (needsUpdate) {
        writeLock.lock(); // DEADLOCK — read lock is still held
    }
} finally { readLock.unlock(); }

// CORRECT — release read lock first, then acquire write lock, re-validate state
readLock.lock();
BigDecimal current;
try { current = rates.get(pair); }
finally { readLock.unlock(); }

if (current == null) {
    writeLock.lock();
    try {
        if (rates.get(pair) == null) { // re-check — another thread may have written
            rates.put(pair, fetchFromProvider(pair));
        }
    } finally { writeLock.unlock(); }
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Write starvation | If reads are constant, the write lock can never be acquired | Use `new ReentrantReadWriteLock(true)` (fair mode) or architect writes to happen in a quiet window |
| Lock downgrade works, upgrade doesn't | A write-holding thread can acquire the read lock before releasing write; the reverse causes deadlock | Downgrade: hold write lock, acquire read lock, release write lock. Upgrade: release read, acquire write, re-validate. |
| Overhead vs `synchronized` when writes are common | Maintaining reader count and writer queue costs more than a simple monitor | Only use `ReadWriteLock` when reads genuinely dominate (>90% reads). Otherwise `synchronized` is faster. |
| Two separate lock objects — easy to accidentally mix | `readLock()` and `writeLock()` are different objects from the same `rwLock` instance | Assign to clearly named fields: `private final Lock readLock = rwLock.readLock()` |

---

---

## § 4 — `StampedLock` — Optimistic Reads

**Use when:** Reads are so dominant that even the read lock's overhead is measurable, and you want to attempt a lock-free read that falls back to a full read lock only on conflict.

```
Optimistic read:  No lock acquired — just read and validate afterwards
Read lock:        Pessimistic — same as ReadWriteLock read lock
Write lock:       Exclusive — same as ReadWriteLock write lock
```

### Three-mode usage — full pattern

```java
public class RealTimeFxService {

    private final StampedLock sl = new StampedLock();
    private double gbpEurRate = 1.17;

    // ── Optimistic read (most common, no locking overhead)
    public double getRate() {
        long stamp = sl.tryOptimisticRead();          // step 1: note current write version
        double value = gbpEurRate;                    // step 2: read (may be stale)
        if (!sl.validate(stamp)) {                    // step 3: was a write interleaved?
            stamp = sl.readLock();                    // step 4: fall back to real read lock
            try { value = gbpEurRate; }
            finally { sl.unlockRead(stamp); }
        }
        return value;
    }

    // ── Write (exclusive)
    public void updateRate(double newRate) {
        long stamp = sl.writeLock();
        try { gbpEurRate = newRate; }
        finally { sl.unlockWrite(stamp); }
    }

    // ── Lock downgrade: write → read (hold read while releasing write)
    public double updateAndRead(double newRate) {
        long stamp = sl.writeLock();
        try {
            gbpEurRate = newRate;
            stamp = sl.tryConvertToReadLock(stamp);   // downgrade atomically
            if (stamp == 0L) stamp = sl.readLock();   // conversion failed — get read lock manually
            return gbpEurRate;
        } finally { sl.unlock(stamp); }
    }
}
```

### Optimistic read validity window

```
Timeline:
  Thread A:  tryOptimisticRead() → stamp=5 ──── read value ──── validate(5) ✅ return
  Thread B:                              writeLock() → change → unlockWrite() ← stamp now 7
  Thread A:  tryOptimisticRead() → stamp=7 ──── read value ──── validate(7) ✅ return
  Thread A:  tryOptimisticRead() → stamp=7 ─ [B writes mid-read] ─ validate(7) ❌ → fall back
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| **Not reentrant** | No owner tracking — acquiring twice from same thread causes deadlock | Never call any method that acquires the same `StampedLock` from within a locked section. Wrap fully in a service class. |
| Stamps are invalidated by write, even if value didn't change | All writes increment the stamp — no per-field tracking | Acceptable. The optimistic path is still fast — the fallback is rare when writes are infrequent. |
| Optimistic read is useless when writes are frequent | Frequent write invalidations force constant fallback to read lock | Benchmark first. If optimistic reads fail >10% of the time, switch to `ReadWriteLock`. |
| No condition variable support | `StampedLock` has no `Condition` equivalent | Use `ReentrantLock` + `Condition` when you need await/signal semantics. |
| Complex API — stamp misuse corrupts the lock | Stamps must match exactly; wrong stamp in `unlock*()` throws | Encapsulate behind a private helper. Never expose stamps to callers. |
| Cannot interrupt a waiting write | `writeLock()` blocks without interruption | Use `writeLockInterruptibly()` for cancellable writes. |

---

---

## § 5 — Deadlock: Patterns and Cures

Deadlock occurs when two or more threads each hold a lock the other needs. In a payment system this can silently freeze all transfers.

### The four conditions (all must hold for deadlock)

| Condition | What it means | How to break it |
|---|---|---|
| Mutual exclusion | Resource held exclusively | Use lock-free structures (§ 0) |
| Hold and wait | Thread holds lock A while waiting for B | Acquire all locks at once, or use `tryLock` with backoff |
| No preemption | Locks aren't forcibly taken | Use `tryLock(timeout)` — let threads give up |
| Circular wait | A → B → A cycle | **Lock ordering** — always acquire in the same global order |

### Cure 1: Lock ordering — deterministic acquisition order

```java
// Thread 1: debit Alice, credit Bob
// Thread 2: debit Bob,   credit Alice
// Without ordering → deadlock. With ordering → safe.

public void transfer(Account from, Account to, long amount) {
    // Always lock the account with the lexicographically smaller ID first
    Account first  = from.getId().compareTo(to.getId()) < 0 ? from : to;
    Account second = first == from ? to : from;

    synchronized (first) {
        synchronized (second) {
            from.debit(amount);
            to.credit(amount);
        }
    }
}

// Why it works: no two threads can form a cycle — they always agree on acquisition order
```

### Cure 2: `tryLock` with timeout and backoff — break hold-and-wait

```java
private static final long LOCK_TIMEOUT_MS = 50;

public boolean transfer(Account from, Account to, long amount)
        throws InterruptedException {

    int attempts = 0;
    while (attempts++ < 3) {
        boolean fromLocked = false, toLocked = false;
        try {
            fromLocked = from.getLock().tryLock(LOCK_TIMEOUT_MS, MILLISECONDS);
            toLocked   = to.getLock().tryLock(LOCK_TIMEOUT_MS, MILLISECONDS);

            if (fromLocked && toLocked) {
                from.debit(amount);
                to.credit(amount);
                return true;
            }
        } finally {
            if (toLocked)   to.getLock().unlock();
            if (fromLocked) from.getLock().unlock();
        }
        // Exponential backoff with jitter before retry
        Thread.sleep(10 * attempts + (long)(Math.random() * 10));
    }
    throw new TransferConflictException("Could not acquire locks after 3 attempts");
}
```

### Cure 3: Push locking to the database — stateless service layer

```java
// Preferred at FinTech's scale — no JVM-level locks needed across pods
// PostgreSQL row-level lock prevents double-spend across all service instances

@Transactional(isolation = SERIALIZABLE)
public void transfer(String fromId, String toId, long amount) {
    // SELECT FOR UPDATE acquires a DB row lock — released at transaction commit
    Account from = accountRepo.findByIdForUpdate(fromId); // SELECT ... FOR UPDATE
    Account to   = accountRepo.findByIdForUpdate(toId);

    from.debit(amount);
    to.credit(amount);

    accountRepo.save(from);
    accountRepo.save(to);
    // Locks released on transaction commit — no JVM lock held across network calls
}
```

| Approach | Pros | Cons | Best for |
|---|---|---|---|
| Lock ordering | Zero overhead, no retries | Requires a global total order on all lockable objects | Single-JVM, known object set |
| `tryLock` + backoff | Works for any lock graph, handles unknown ordering | Adds retry complexity; jitter required to avoid livelock | Multi-object transfers with dynamic participants |
| Database `SELECT FOR UPDATE` | Spans all JVM instances; transactional guarantees | DB becomes the bottleneck for lock acquisition | Production distributed systems (FinTech's actual approach) |
| Optimistic versioning | No locking at all until conflict | Requires retry loop at the application level | Low-contention paths; read-heavy with rare conflicts |

---

---

## § 6 — `wait` / `notifyAll` — Object Monitor Conditions

**Use when:** You are already using `synchronized` and need threads to wait for a state change. Prefer `Condition` (§ 7) for all new code.

```java
private final Object monitor = new Object();
private boolean paymentReady = false;

// Consumer thread — wait until payment is ready
public Payment awaitPayment() throws InterruptedException {
    synchronized (monitor) {
        while (!paymentReady) {   // ALWAYS while — never if (spurious wakeups)
            monitor.wait();       // atomically releases lock and suspends thread
        }
        paymentReady = false;
        return currentPayment;
    }
}

// Producer thread — signal the consumer
public void publish(Payment payment) {
    synchronized (monitor) {
        currentPayment = payment;
        paymentReady = true;
        monitor.notifyAll();      // wake all waiters — they re-check the condition
    }
}
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Spurious wakeups | JVM/OS may wake threads without `notifyAll()` being called | **Always** re-check condition in a `while` loop, never `if` |
| `notify()` vs `notifyAll()` | `notify()` wakes one arbitrary thread — can leave correct waiters sleeping | Use `notifyAll()` unless you are certain all waiters are interchangeable and exactly one should run |
| Must hold the lock before calling `wait()` | JVM contract — `IllegalMonitorStateException` otherwise | Always call `wait()`/`notify()` inside a `synchronized` block on the same object |
| Missed signal if producer runs before consumer waits | `notifyAll()` before `wait()` — the wake-up is lost | Design so state (not signal) drives the `while` condition. The consumer checks state, not whether it was notified. |
| Timeout version (`wait(millis)`) doesn't tell you why it returned | Returns on signal, spurious wakeup, or timeout — all look the same | Check the condition again after `wait(millis)` to distinguish timeout from genuine signal |

---

---

## § 7 — `Condition` — Explicit Wait/Signal (Preferred)

**Use when:** You need await/signal semantics, multiple independent wait sets on the same lock, or timeouts that distinguish wakeup reasons.

```java
public class BoundedPaymentQueue {

    private final ReentrantLock lock      = new ReentrantLock();
    private final Condition     notFull   = lock.newCondition();
    private final Condition     notEmpty  = lock.newCondition();

    private final Payment[]     buffer;
    private int                 head, tail, count;

    public BoundedPaymentQueue(int capacity) { buffer = new Payment[capacity]; }

    // Producer — blocks if full
    public void put(Payment payment) throws InterruptedException {
        lock.lock();
        try {
            while (count == buffer.length) notFull.await();  // wait for space
            buffer[tail++ % buffer.length] = payment;
            count++;
            notEmpty.signal();  // wake exactly one consumer (safe — all consumers are equivalent)
        } finally { lock.unlock(); }
    }

    // Consumer — blocks if empty
    public Payment take() throws InterruptedException {
        lock.lock();
        try {
            while (count == 0) notEmpty.await();  // wait for work
            Payment p = buffer[head++ % buffer.length];
            count--;
            notFull.signal();  // wake exactly one producer
            return p;
        } finally { lock.unlock(); }
    }
}
```

### `Condition` vs `wait/notifyAll`

| Feature | `wait` / `notifyAll` | `Condition.await` / `signal` |
|---|---|---|
| Multiple wait sets on one lock | ❌ All waiters share one set | ✅ `lock.newCondition()` — one per logical condition |
| Timed await with wakeup reason | ❌ `wait(ms)` can't distinguish timeout from signal | ✅ `await(timeout, unit)` returns `false` on timeout |
| Interruptible await | ❌ `wait()` swallows interrupts | ✅ `await()` throws `InterruptedException` cleanly |
| Signal one from a specific set | ❌ `notify()` picks arbitrarily from all waiters | ✅ `signal()` picks from the specific `Condition`'s set |

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Still requires `while` loop for spurious wakeups | JVM contract — same as `wait()` | `await()` documentation explicitly says: always check condition in a loop |
| `signal()` vs `signalAll()` same subtlety as `notify()` | Waking one thread requires certainty all waiters are equivalent | Use `signalAll()` when in doubt; use `signal()` only in classic single-condition producer-consumer patterns |
| Must hold the lock to call `await()` / `signal()` | Same as `wait()` / `notify()` | Always verify you are inside `lock.lock()` ... `lock.unlock()` |

---

---

## § 8 — `LockSupport` — Low-Level Thread Parking

**Use when:** You are implementing a custom synchroniser and need direct thread park/unpark. Not for application code — this is the primitive under `ReentrantLock`, `CountDownLatch`, and `CompletableFuture`.

```java
// LockSupport is what java.util.concurrent is built on top of
LockSupport.park();           // suspend current thread (no lock required)
LockSupport.unpark(thread);   // resume a specific thread — can happen before park()
LockSupport.parkNanos(1000);  // park with a timeout

// Key difference vs wait(): unpark() can be called BEFORE park()
// The "permit" is stored — park() returns immediately if a permit is available
```

| Trade-off | Why it happens | How to overcome |
|---|---|---|
| Spurious wakeups | Same JVM contract | Always check condition after `park()` returns |
| Not reentrant | `unpark()` stores at most one permit | Don't call `unpark()` multiple times expecting multiple `park()` calls to proceed — only one permit is buffered |
| No built-in timeout feedback | `parkNanos()` returns for both timeout and unpark | Check `System.nanoTime()` after return to distinguish |
| Application code should not use this | Requires deep understanding of JMM and thread lifecycle | Use `ReentrantLock`, `Semaphore`, `CountDownLatch`, or `CompletableFuture` instead |

---

---

## Locks Under Java 21 Virtual Threads

Virtual threads change which lock primitives are safe to use. This is an active concern for FinTech's Java 21 stack.

```
Virtual thread encounters blocking operation
│
├── I/O (socket, file, network) ────── JVM parks the virtual thread, carrier thread is freed ✅
├── ReentrantLock.lock()          ────── JVM parks the virtual thread, carrier thread is freed ✅
├── Condition.await()             ────── JVM parks the virtual thread, carrier thread is freed ✅
├── LockSupport.park()            ────── JVM parks the virtual thread, carrier thread is freed ✅
│
└── synchronized block/method    ────── Virtual thread is PINNED to carrier thread ⚠️
    └── Carrier thread is blocked — defeats the purpose of virtual threads
        └── Fix: replace synchronized with ReentrantLock in any hot path
```

### Diagnosing pinning

```java
// JVM flag to log pinning events during development
// -Djdk.tracePinnedThreads=full

// Or detect programmatically in tests
try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
    executor.submit(() -> {
        synchronized (this) {          // ← pinning event logged here
            Thread.sleep(100);
        }
    }).get();
}
```

### Conversion pattern: `synchronized` → `ReentrantLock`

```java
// Before (pins carrier thread in Java 21)
public synchronized boolean debit(long amount) {
    if (balance < amount) return false;
    balance -= amount;
    return true;
}

// After (virtual-thread safe)
private final ReentrantLock lock = new ReentrantLock();

public boolean debit(long amount) {
    lock.lock();
    try {
        if (balance < amount) return false;
        balance -= amount;
        return true;
    } finally {
        lock.unlock();
    }
}
```

| Primitive | Virtual-thread safe | Note |
|---|---|---|
| `synchronized` | ⚠️ Causes pinning | Replace with `ReentrantLock` in I/O or high-concurrency paths |
| `ReentrantLock` | ✅ | Preferred lock in Java 21 codebases |
| `ReentrantReadWriteLock` | ✅ | Safe — uses `LockSupport` internally |
| `StampedLock` | ✅ | Safe — same |
| `Object.wait()` | ⚠️ Causes pinning | Replace with `Condition.await()` |
| `LockSupport.park()` | ✅ | Parks the virtual thread, not the carrier |
| `Semaphore.acquire()` | ✅ | Safe — uses `LockSupport` |

---

---

## Master Decision Summary

| Situation | Lock choice | Avoid |
|---|---|---|
| Single variable, one writer, visibility only | `volatile` | Any lock — overkill |
| Atomic counter or CAS on a number | `AtomicLong` | `synchronized` on a wrapper object |
| Simple critical section, low contention | `synchronized` | Anything more complex unless needed |
| Need timeout on lock acquisition | `ReentrantLock.tryLock(timeout)` | `synchronized` |
| Need to cancel a waiting thread | `ReentrantLock.lockInterruptibly()` | `synchronized` |
| Many readers, rare writers | `ReentrantReadWriteLock` | `synchronized` (unnecessarily serialises reads) |
| Reads completely dominate, writes very rare | `StampedLock` (optimistic read) | `ReentrantReadWriteLock` (still takes a read lock) |
| Wait for a state change inside `synchronized` | `Condition` via `ReentrantLock` | `wait()`/`notifyAll()` (harder to use correctly) |
| Deadlock prevention across two objects | Lock ordering or `tryLock` backoff | Nested `synchronized` without a consistent order |
| Distributed lock across JVM instances | Database `SELECT FOR UPDATE` or Redis `SETNX` | JVM-level lock (invisible to other pods) |
| Java 21 virtual threads | `ReentrantLock` everywhere | `synchronized` (causes carrier thread pinning) |
| Custom synchroniser internals | `LockSupport.park/unpark` | Direct use in application code |

---

*Designed for FinTech Round 2 — Java 21, virtual threads, PostgreSQL, Kubernetes context.*

# FinTech Java Interview — 5 Live Coding Problems

> Each problem mirrors the real interview structure:
> **Clarify → Plan → Test (TDD) → Implement → Thread-safe → Improve**
>
> Work through each stage sequentially. Don't skip ahead.
> Target: finish at least stages 1–2 within 30–35 minutes.

---

## How to use this guide

Every problem is broken into stages labeled **[Stage N]**. Each stage has:

- **What the interviewer says** — the prompt as it would be delivered verbally
- **Clarifying questions to ask** — what you should verbalize before touching the keyboard
- **Your planned approach** — say this out loud before coding
- **Test first** — write the failing test before the implementation
- **Implementation** — the minimal solution to pass the tests
- **Evolving stages** — concurrency, persistence, edge cases added one at a time
- **Trade-off discussion** — the follow-up conversation the interviewer expects

---

---

# Problem 1 — In-Memory Account Ledger

> **Theme:** Core domain modelling, value objects, concurrency, idempotency
> **Difficulty:** Medium
> **FinTech relevance:** This is the canonical payment domain — expect something in this family

---

## Stage 1 — Basic debit and credit

### What the interviewer says

> *"Implement an `Account` class. It should hold a balance and support `debit` and `credit` operations. Balances can't go negative."*

---

### Clarifying questions to ask

Before writing a single line, say these out loud:

1. *"What type should the balance be — `long` in minor units (pence/cents), `BigDecimal`, or a custom `Money` type?"*
2. *"Should `debit` throw an exception or return a result when funds are insufficient?"*
3. *"Do we need to store transaction history at this stage, or just the current balance?"*
4. *"Is currency relevant now, or can I start with a single-currency account?"*

**Reasonable assumptions to state:**
- Balance stored as `long` in minor units (e.g. pence) — avoids floating-point issues
- `debit` throws `InsufficientFundsException` on failure
- Single currency, no transaction history yet

---

### Your planned approach (say this out loud)

> *"I'll create an `Account` value object with a `long` balance field. I'll keep it simple — no concurrency yet. I'll start with a failing test for the happy path, then add the exception case."*

---

### Test first

```java
class AccountTest {

    @Test
    void credit_increasesBalance() {
        Account account = new Account("acc-1", 1000L);
        account.credit(500L);
        assertThat(account.getBalance()).isEqualTo(1500L);
    }

    @Test
    void debit_reducesBalance() {
        Account account = new Account("acc-1", 1000L);
        account.debit(400L);
        assertThat(account.getBalance()).isEqualTo(600L);
    }

    @Test
    void debit_exactBalance_reducesToZero() {
        Account account = new Account("acc-1", 1000L);
        account.debit(1000L);
        assertThat(account.getBalance()).isZero();
    }

    @Test
    void debit_throwsWhenInsufficientFunds() {
        Account account = new Account("acc-1", 100L);
        assertThatThrownBy(() -> account.debit(200L))
            .isInstanceOf(InsufficientFundsException.class)
            .hasMessageContaining("acc-1");
    }

    @Test
    void credit_rejectsNegativeAmount() {
        Account account = new Account("acc-1", 100L);
        assertThatThrownBy(() -> account.credit(-50L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

---

### Implementation

```java
public class Account {

    private final String id;
    private long balance;

    public Account(String id, long initialBalance) {
        if (initialBalance < 0) throw new IllegalArgumentException("Initial balance cannot be negative");
        this.id = id;
        this.balance = initialBalance;
    }

    public void credit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Credit amount must be positive");
        balance += amount;
    }

    public void debit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Debit amount must be positive");
        if (balance < amount) throw new InsufficientFundsException(id, balance, amount);
        balance -= amount;
    }

    public long getBalance() { return balance; }
    public String getId()    { return id; }
}

public class InsufficientFundsException extends RuntimeException {
    public InsufficientFundsException(String accountId, long balance, long requested) {
        super("Account %s has insufficient funds: balance=%d, requested=%d"
              .formatted(accountId, balance, requested));
    }
}
```

---

## Stage 2 — Thread safety

### What the interviewer says

> *"Now imagine thousands of concurrent payment requests hitting the same account. Make it thread-safe."*

---

### Clarifying questions to ask

1. *"What's the expected concurrency level — tens, hundreds, thousands of threads?"*
2. *"Is read performance critical, or is this write-heavy?"*
3. *"Can I use `synchronized` first and then discuss lock-free alternatives?"*

---

### Your planned approach (say this out loud)

> *"I'll start with `synchronized` — it's the simplest correct solution. Then I'll show a CAS-based lock-free version using `AtomicLong` and discuss the trade-offs."*

---

### Concurrency test first

```java
@Test
void concurrentDebits_neverProduceNegativeBalance() throws InterruptedException {
    Account account = new Account("acc-1", 10_000L);
    ExecutorService executor = Executors.newFixedThreadPool(50);
    CountDownLatch latch = new CountDownLatch(200);

    for (int i = 0; i < 200; i++) {
        executor.submit(() -> {
            try {
                account.debit(100L);
            } catch (InsufficientFundsException ignored) {
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(5, TimeUnit.SECONDS);
    assertThat(account.getBalance()).isGreaterThanOrEqualTo(0L);
}

@Test
void concurrentCreditsAndDebits_balanceIsConsistent() throws InterruptedException {
    Account account = new Account("acc-1", 0L);
    ExecutorService executor = Executors.newFixedThreadPool(20);
    CountDownLatch latch = new CountDownLatch(1000);

    // 500 credits of 100, 500 debits of 100 → expected balance: 0
    for (int i = 0; i < 1000; i++) {
        final boolean isCredit = i % 2 == 0;
        executor.submit(() -> {
            try {
                if (isCredit) account.credit(100L);
                else          account.debit(100L);
            } catch (InsufficientFundsException ignored) {
            } finally {
                latch.countDown();
            }
        });
    }

    latch.await(5, TimeUnit.SECONDS);
    assertThat(account.getBalance()).isGreaterThanOrEqualTo(0L);
}
```

---

### Implementation — Option A: synchronized (simple, start here)

```java
public class Account {

    private final String id;
    private long balance;

    public Account(String id, long initialBalance) {
        this.id = id;
        this.balance = initialBalance;
    }

    public synchronized void credit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        balance += amount;
    }

    public synchronized void debit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        if (balance < amount) throw new InsufficientFundsException(id, balance, amount);
        balance -= amount;
    }

    public synchronized long getBalance() { return balance; }
}
```

### Implementation — Option B: lock-free with AtomicLong (discuss as upgrade)

```java
public class Account {

    private final String id;
    private final AtomicLong balance;

    public Account(String id, long initialBalance) {
        this.id = id;
        this.balance = new AtomicLong(initialBalance);
    }

    public void credit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        balance.addAndGet(amount); // atomic, no lock needed
    }

    public void debit(long amount) {
        if (amount <= 0) throw new IllegalArgumentException("Amount must be positive");
        long current;
        do {
            current = balance.get();
            if (current < amount) throw new InsufficientFundsException(id, current, amount);
        } while (!balance.compareAndSet(current, current - amount)); // retry on conflict
    }

    public long getBalance() { return balance.get(); }
}
```

---

### Trade-off discussion to lead

| | `synchronized` | `AtomicLong` CAS |
|---|---|---|
| Simplicity | ✅ Simple | ⚠️ CAS loop subtle |
| Throughput under high contention | ⚠️ Threads queue | ✅ No blocking |
| Fairness | Configurable via `ReentrantLock(true)` | ❌ Starvation possible |
| Compound operations | ✅ Easy | ⚠️ Complex |
| Virtual Threads (Java 21) | ⚠️ `synchronized` pins carrier thread | ✅ No pinning |

> *"For Java 21 virtual threads, I'd prefer `ReentrantLock` over `synchronized` to avoid carrier thread pinning. At very high contention, the CAS approach outperforms locking because threads never block — they simply retry."*

---

## Stage 3 — Domain events and audit trail

### What the interviewer says

> *"We need to audit every balance change. Add a transaction history."*

---

### Implementation

```java
// Immutable domain event — use Java 16+ record
public record BalanceEvent(
    String transactionId,
    EventType type,
    long amount,
    long balanceAfter,
    Instant occurredAt
) {
    public enum EventType { CREDIT, DEBIT }
}

public class Account {

    private final String id;
    private final AtomicLong balance;
    private final List<BalanceEvent> events = new CopyOnWriteArrayList<>();

    public void debit(long amount) {
        // ... (same CAS logic as before)
        events.add(new BalanceEvent(
            UUID.randomUUID().toString(),
            BalanceEvent.EventType.DEBIT,
            amount,
            balance.get(),
            Instant.now()
        ));
    }

    // Pull and clear events (used by persistence layer to publish domain events)
    public List<BalanceEvent> drainEvents() {
        List<BalanceEvent> snapshot = List.copyOf(events);
        events.clear();
        return snapshot;
    }
}
```

> **Note:** `CopyOnWriteArrayList` is safe for concurrent append + occasional drain. If event volume is very high, use a `ConcurrentLinkedQueue` instead.

---

---

# Problem 2 — Transfer Service Between Accounts

> **Theme:** Deadlock prevention, atomicity across two objects, Saga pattern
> **Difficulty:** Medium-Hard
> **FinTech relevance:** The "move money from A to B" problem is core to any payment company

---

## Stage 1 — Basic transfer

### What the interviewer says

> *"Write a `TransferService` that moves money between two accounts atomically."*

---

### Clarifying questions to ask

1. *"Both accounts are in-memory for now, or do we need to model persistence?"*
2. *"Should partial failures be silent or thrown? If A debits but B credit fails, what happens?"*
3. *"Do we need idempotency — what if this method is called twice with the same transfer?"*

---

### Test first

```java
class TransferServiceTest {

    private TransferService service;
    private Account alice;
    private Account bob;

    @BeforeEach
    void setUp() {
        service = new TransferService();
        alice   = new Account("alice", 1000L);
        bob     = new Account("bob",   500L);
    }

    @Test
    void transfer_movesMoneyBetweenAccounts() {
        service.transfer(alice, bob, 300L);
        assertThat(alice.getBalance()).isEqualTo(700L);
        assertThat(bob.getBalance()).isEqualTo(800L);
    }

    @Test
    void transfer_failsWhenInsufficientFunds() {
        assertThatThrownBy(() -> service.transfer(alice, bob, 2000L))
            .isInstanceOf(InsufficientFundsException.class);
        // Both balances must be unchanged — atomicity
        assertThat(alice.getBalance()).isEqualTo(1000L);
        assertThat(bob.getBalance()).isEqualTo(500L);
    }

    @Test
    void transfer_toSameAccount_throws() {
        assertThatThrownBy(() -> service.transfer(alice, alice, 100L))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
```

---

### Implementation — Stage 1 (single-threaded)

```java
public class TransferService {

    public void transfer(Account from, Account to, long amount) {
        if (from == to) throw new IllegalArgumentException("Cannot transfer to same account");
        if (amount <= 0) throw new IllegalArgumentException("Transfer amount must be positive");

        from.debit(amount);   // throws InsufficientFundsException — to.credit never reached
        to.credit(amount);
    }
}
```

---

## Stage 2 — Concurrent transfers without deadlock

### What the interviewer says

> *"Thread A is transferring Alice → Bob while Thread B is transferring Bob → Alice simultaneously. How do you prevent a deadlock?"*

---

### Deadlock reproduction test (write this to show you understand the problem)

```java
@Test
void concurrentTransfers_shouldNotDeadlock() throws InterruptedException {
    Account alice = new Account("alice", 10_000L);
    Account bob   = new Account("bob",   10_000L);
    TransferService service = new TransferService();

    CountDownLatch latch = new CountDownLatch(2);
    AtomicReference<Throwable> error = new AtomicReference<>();

    Thread t1 = new Thread(() -> {
        try { for (int i = 0; i < 100; i++) service.transfer(alice, bob, 10L); }
        catch (Throwable e) { error.set(e); }
        finally { latch.countDown(); }
    });

    Thread t2 = new Thread(() -> {
        try { for (int i = 0; i < 100; i++) service.transfer(bob, alice, 10L); }
        catch (Throwable e) { error.set(e); }
        finally { latch.countDown(); }
    });

    t1.start(); t2.start();
    assertThat(latch.await(5, TimeUnit.SECONDS)).isTrue(); // fails if deadlocked
    assertThat(error.get()).isNull();
    assertThat(alice.getBalance() + bob.getBalance()).isEqualTo(20_000L); // invariant
}
```

---

### Implementation — deadlock-safe via lock ordering

```java
public class TransferService {

    public void transfer(Account from, Account to, long amount) {
        if (from.getId().equals(to.getId()))
            throw new IllegalArgumentException("Cannot transfer to same account");

        // Always acquire locks in a deterministic order to prevent deadlock
        Account first  = from.getId().compareTo(to.getId()) < 0 ? from : to;
        Account second = from.getId().compareTo(to.getId()) < 0 ? to   : from;

        synchronized (first) {
            synchronized (second) {
                from.debit(amount);
                to.credit(amount);
            }
        }
    }
}
```

---

### Alternative — tryLock with timeout (discuss as upgrade)

```java
public void transfer(Account from, Account to, long amount) {
    // Assumes Account exposes a ReentrantLock
    boolean fromLocked = false, toLocked = false;
    try {
        fromLocked = from.getLock().tryLock(100, TimeUnit.MILLISECONDS);
        toLocked   = to.getLock().tryLock(100, TimeUnit.MILLISECONDS);

        if (!fromLocked || !toLocked) throw new TransferConflictException("Could not acquire locks, retry");

        from.debit(amount);
        to.credit(amount);
    } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
    } finally {
        if (toLocked)   to.getLock().unlock();
        if (fromLocked) from.getLock().unlock();
    }
}
```

---

### Trade-off discussion to lead

> *"Lock ordering guarantees deadlock freedom but requires a global total order on account IDs. `tryLock` with timeout is more flexible and works when ordering is impractical, but it introduces retry complexity and potential livelock under extreme contention. In a real FinTech service, I'd go further — use database-level `SELECT FOR UPDATE` so the application layer is stateless and doesn't hold JVM-level locks across service instances."*

---

---

# Problem 3 — Rate Limiter

> **Theme:** Token bucket algorithm, concurrency, time-based state
> **Difficulty:** Medium
> **FinTech relevance:** Every FinTech API endpoint has per-user rate limits

---

## Stage 1 — Fixed window rate limiter

### What the interviewer says

> *"Implement a rate limiter that allows a maximum of N requests per user per minute."*

---

### Clarifying questions to ask

1. *"Fixed window (reset every minute) or sliding window (rolling 60-second window)?"*
2. *"Should the limiter be per-user, per-IP, or global?"*
3. *"What happens when the limit is exceeded — return boolean, throw exception, or return a retry-after duration?"*
4. *"Is this in-memory only, or should I model it as if backed by Redis?"*

**State your assumption:** *"I'll start with a fixed window, in-memory, per-user limiter that returns a boolean."*

---

### Test first

```java
class RateLimiterTest {

    @Test
    void allowsRequestsUpToLimit() {
        RateLimiter limiter = new RateLimiter(5);  // 5 per minute
        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("user-1")).isTrue();
        }
    }

    @Test
    void blocksRequestsBeyondLimit() {
        RateLimiter limiter = new RateLimiter(3);
        limiter.tryAcquire("user-1");
        limiter.tryAcquire("user-1");
        limiter.tryAcquire("user-1");
        assertThat(limiter.tryAcquire("user-1")).isFalse();
    }

    @Test
    void limitsArePerUser() {
        RateLimiter limiter = new RateLimiter(2);
        assertThat(limiter.tryAcquire("user-1")).isTrue();
        assertThat(limiter.tryAcquire("user-1")).isTrue();
        assertThat(limiter.tryAcquire("user-1")).isFalse(); // user-1 exhausted
        assertThat(limiter.tryAcquire("user-2")).isTrue();  // user-2 unaffected
    }
}
```

---

### Implementation — fixed window

```java
public class RateLimiter {

    private final int maxRequests;
    private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

    public RateLimiter(int maxRequests) {
        this.maxRequests = maxRequests;
    }

    public boolean tryAcquire(String userId) {
        WindowCounter counter = counters.computeIfAbsent(userId, k -> new WindowCounter());
        return counter.increment(maxRequests);
    }

    private static class WindowCounter {
        private final AtomicLong count     = new AtomicLong(0);
        private volatile long   windowStart = currentMinute();

        synchronized boolean increment(int limit) {
            long now = currentMinute();
            if (now != windowStart) {   // new minute — reset
                windowStart = now;
                count.set(0);
            }
            return count.incrementAndGet() <= limit;
        }

        private static long currentMinute() {
            return System.currentTimeMillis() / 60_000;
        }
    }
}
```

---

## Stage 2 — Token bucket (handles bursts correctly)

### What the interviewer says

> *"A user sends 5 requests in the last second of a window and 5 more in the first second of the next — your limiter allows 10 in 2 seconds. How do you fix it?"*

---

### Test for burst problem

```java
@Test
void tokenBucket_smoothsOverWindowBoundary() {
    // This test is conceptual — in a unit test we'd inject a clock
    // The key property: at most `maxPerMinute` tokens ever available
    TokenBucketRateLimiter limiter = new TokenBucketRateLimiter(5, Duration.ofMinutes(1));
    // Fill the bucket
    for (int i = 0; i < 5; i++) assertThat(limiter.tryAcquire("user-1")).isTrue();
    // No more tokens immediately after
    assertThat(limiter.tryAcquire("user-1")).isFalse();
}
```

---

### Implementation — token bucket with injectable clock

```java
public class TokenBucketRateLimiter {

    private final int capacity;
    private final long refillIntervalNanos;
    private final Clock clock;
    private final ConcurrentHashMap<String, Bucket> buckets = new ConcurrentHashMap<>();

    public TokenBucketRateLimiter(int capacity, Duration refillInterval) {
        this(capacity, refillInterval, Clock.systemUTC());
    }

    // Inject clock for testability
    public TokenBucketRateLimiter(int capacity, Duration refillInterval, Clock clock) {
        this.capacity = capacity;
        this.refillIntervalNanos = refillInterval.toNanos();
        this.clock = clock;
    }

    public boolean tryAcquire(String userId) {
        Bucket bucket = buckets.computeIfAbsent(userId, k -> new Bucket(capacity, clock.instant()));
        return bucket.tryConsume();
    }

    private class Bucket {
        private long tokens;
        private Instant lastRefill;

        Bucket(long tokens, Instant now) {
            this.tokens = tokens;
            this.lastRefill = now;
        }

        synchronized boolean tryConsume() {
            Instant now = clock.instant();
            long elapsed = Duration.between(lastRefill, now).toNanos();
            long tokensToAdd = (elapsed / refillIntervalNanos) * capacity;

            if (tokensToAdd > 0) {
                tokens = Math.min(capacity, tokens + tokensToAdd);
                lastRefill = now;
            }

            if (tokens <= 0) return false;
            tokens--;
            return true;
        }
    }
}
```

---

### Trade-off discussion to lead

> *"Fixed window is O(1) per call with minimal memory, but the boundary burst problem is real for payment APIs — a user could front-load a double allowance. Token bucket solves this with smooth refill semantics. In production I'd back this with Redis `INCRBY` + a TTL so the limiter scales horizontally across service instances — a JVM-local limiter is lost on pod restart."*

---

---

# Problem 4 — Idempotent Payment Processor

> **Theme:** Idempotency keys, deduplication, retry safety
> **Difficulty:** Medium-Hard
> **FinTech relevance:** Every payment API FinTech exposes must handle client retries safely

---

## Stage 1 — Basic idempotent processor

### What the interviewer says

> *"A client sends a payment request and the connection drops before receiving a response. They retry. Implement a payment processor that guarantees the payment is processed exactly once."*

---

### Clarifying questions to ask

1. *"Is the idempotency key provided by the client or generated server-side?"*
2. *"What's the retention period for idempotency records?"*
3. *"Should repeated requests with the same key return the original result or throw?"*
4. *"What's the definition of 'same request' — just the key, or key + payload hash?"*

**State your assumption:** *"Client provides the key. Same key always returns the same result, even if the payload differs. Retention is 7 days. I'll model storage as an in-memory map for now."*

---

### Test first

```java
class IdempotentPaymentProcessorTest {

    private IdempotentPaymentProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new IdempotentPaymentProcessor(new InMemoryPaymentHandler());
    }

    @Test
    void sameIdempotencyKey_returnsSameResult() {
        PaymentRequest req = new PaymentRequest("idem-key-1", "alice", "bob", 100L);

        PaymentResult first  = processor.process(req);
        PaymentResult second = processor.process(req);   // retry

        assertThat(second).isEqualTo(first);             // same result, not re-processed
    }

    @Test
    void differentKeys_areBothProcessed() {
        PaymentResult r1 = processor.process(new PaymentRequest("key-1", "alice", "bob", 100L));
        PaymentResult r2 = processor.process(new PaymentRequest("key-2", "alice", "bob", 100L));

        assertThat(r1.paymentId()).isNotEqualTo(r2.paymentId());
    }

    @Test
    void failedPayment_isNotReplayed_asSuccess() {
        // A payment that fails should return the same failure on retry
        PaymentRequest req = new PaymentRequest("key-fail", "alice", "bob", Long.MAX_VALUE);

        PaymentResult first  = processor.process(req);
        PaymentResult second = processor.process(req);

        assertThat(first.status()).isEqualTo(PaymentStatus.FAILED);
        assertThat(second.status()).isEqualTo(PaymentStatus.FAILED);
    }
}
```

---

### Supporting types

```java
public record PaymentRequest(
    String idempotencyKey,
    String fromAccount,
    String toAccount,
    long   amountMinorUnits
) {}

public record PaymentResult(
    String        paymentId,
    PaymentStatus status,
    Instant       processedAt
) {}

public enum PaymentStatus { SUCCESS, FAILED, INSUFFICIENT_FUNDS }
```

---

### Implementation

```java
public class IdempotentPaymentProcessor {

    private final PaymentHandler            handler;
    private final ConcurrentHashMap<String, PaymentResult> store = new ConcurrentHashMap<>();

    public IdempotentPaymentProcessor(PaymentHandler handler) {
        this.handler = handler;
    }

    public PaymentResult process(PaymentRequest request) {
        // computeIfAbsent is atomic — only one thread will call handler for a given key
        return store.computeIfAbsent(
            request.idempotencyKey(),
            key -> handler.execute(request)
        );
    }
}
```

---

## Stage 2 — Handle in-flight deduplication

### What the interviewer says

> *"Two threads arrive simultaneously with the same idempotency key before the first completes. What happens with your implementation?"*

---

### The problem with computeIfAbsent under slow handlers

```java
// computeIfAbsent is safe for fast lambdas but for a slow handler (DB call, HTTP)
// two threads can both enter the lambda if the first hasn't returned yet in some JDK versions.
// More importantly: the mapping function should not throw or have side effects
// that make partial execution observable.

// Safer pattern: use explicit locking per key
public class IdempotentPaymentProcessor {

    private final PaymentHandler                           handler;
    private final ConcurrentHashMap<String, PaymentResult> store   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Object>        locks   = new ConcurrentHashMap<>();

    public PaymentResult process(PaymentRequest request) {
        String key = request.idempotencyKey();

        // Fast path — already processed
        PaymentResult existing = store.get(key);
        if (existing != null) return existing;

        // Slow path — acquire per-key lock, check again, then process
        Object lock = locks.computeIfAbsent(key, k -> new Object());
        synchronized (lock) {
            return store.computeIfAbsent(key, k -> handler.execute(request));
        }
    }
}
```

---

### Trade-off discussion to lead

> *"In production this store would be backed by a database table with a unique constraint on `idempotency_key`. The unique constraint acts as a distributed lock — only one INSERT succeeds across all service instances. The application catches the duplicate key exception and reads back the original result. This is safer than JVM-level locking, which doesn't span multiple pods."*

---

---

# Problem 5 — Concurrent Notification Dispatcher

> **Theme:** Producer-consumer, bounded queues, backpressure, graceful shutdown
> **Difficulty:** Hard
> **FinTech relevance:** FinTech sends millions of push notifications — this is the async worker pattern

---

## Stage 1 — Asynchronous dispatcher

### What the interviewer says

> *"We receive payment events and need to send push notifications asynchronously. Implement a `NotificationDispatcher` that accepts events and processes them without blocking the caller."*

---

### Clarifying questions to ask

1. *"What's the expected event throughput — hundreds or millions per second?"*
2. *"What happens when the downstream notification service is slow — do we drop events, buffer them, or apply backpressure to the caller?"*
3. *"Do we need guaranteed delivery (persist to DB/Kafka before ack) or best-effort?"*
4. *"How many worker threads should process notifications?"*

**State your assumption:** *"Best-effort delivery, bounded in-memory queue, configurable worker threads, block caller on full queue (backpressure). I'll model the notification service as an interface."*

---

### Test first

```java
class NotificationDispatcherTest {

    @Test
    void dispatches_eventToNotificationService() throws InterruptedException {
        NotificationService mockService = mock(NotificationService.class);
        NotificationDispatcher dispatcher = new NotificationDispatcher(mockService, 2, 100);

        dispatcher.dispatch(new PaymentEvent("user-1", "Payment of £10 received"));
        dispatcher.shutdown();

        verify(mockService, timeout(1000)).send(any(PaymentEvent.class));
    }

    @Test
    void processes_multipleEventsInOrder() throws InterruptedException {
        List<PaymentEvent> processed = new CopyOnWriteArrayList<>();
        NotificationDispatcher dispatcher = new NotificationDispatcher(
            event -> processed.add(event), 1, 100  // single worker to preserve order
        );

        dispatcher.dispatch(new PaymentEvent("user-1", "Event 1"));
        dispatcher.dispatch(new PaymentEvent("user-1", "Event 2"));
        dispatcher.dispatch(new PaymentEvent("user-1", "Event 3"));
        dispatcher.shutdown();

        assertThat(processed).extracting(PaymentEvent::message)
            .containsExactly("Event 1", "Event 2", "Event 3");
    }

    @Test
    void blocksCallerWhenQueueFull() throws InterruptedException {
        // Slow service to fill the queue
        NotificationService slowService = event -> Thread.sleep(500);
        NotificationDispatcher dispatcher = new NotificationDispatcher(slowService, 1, 2); // queue=2

        dispatcher.dispatch(new PaymentEvent("u1", "E1")); // taken by worker
        dispatcher.dispatch(new PaymentEvent("u1", "E2")); // in queue
        dispatcher.dispatch(new PaymentEvent("u1", "E3")); // fills queue

        // 4th should block (backpressure) — assert it completes within timeout
        assertThatCode(() -> dispatcher.dispatch(new PaymentEvent("u1", "E4")))
            .doesNotThrowAnyException();
    }
}
```

---

### Implementation

```java
public class NotificationDispatcher {

    private final BlockingQueue<PaymentEvent> queue;
    private final ExecutorService             workers;
    private final NotificationService         service;

    public NotificationDispatcher(NotificationService service, int workerCount, int queueCapacity) {
        this.service = service;
        this.queue   = new ArrayBlockingQueue<>(queueCapacity);
        this.workers = Executors.newFixedThreadPool(workerCount);

        for (int i = 0; i < workerCount; i++) {
            workers.submit(this::processLoop);
        }
    }

    public void dispatch(PaymentEvent event) {
        try {
            queue.put(event); // blocks when full — natural backpressure
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new DispatchInterruptedException(event);
        }
    }

    private void processLoop() {
        while (!Thread.currentThread().isInterrupted()) {
            try {
                PaymentEvent event = queue.poll(100, TimeUnit.MILLISECONDS);
                if (event != null) send(event);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private void send(PaymentEvent event) {
        try {
            service.send(event);
        } catch (Exception e) {
            // In production: dead-letter queue or retry with backoff
            log.error("Failed to send notification for event {}: {}", event, e.getMessage());
        }
    }

    public void shutdown() {
        workers.shutdownNow(); // interrupts processLoop
        try {
            workers.awaitTermination(5, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## Stage 2 — Retry with exponential backoff

### What the interviewer says

> *"The notification service is flaky. Add retry logic with exponential backoff, but don't block the worker."*

---

### Implementation — RetryingNotificationService wrapper

```java
public class RetryingNotificationService implements NotificationService {

    private final NotificationService delegate;
    private final int                 maxAttempts;
    private final long                initialDelayMs;

    public RetryingNotificationService(NotificationService delegate, int maxAttempts, long initialDelayMs) {
        this.delegate       = delegate;
        this.maxAttempts    = maxAttempts;
        this.initialDelayMs = initialDelayMs;
    }

    @Override
    public void send(PaymentEvent event) {
        long delay = initialDelayMs;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                delegate.send(event);
                return; // success
            } catch (Exception e) {
                if (attempt == maxAttempts) {
                    throw new NotificationDeliveryException("Failed after " + maxAttempts + " attempts", e);
                }
                sleepWithJitter(delay);
                delay *= 2; // exponential backoff: 100ms → 200ms → 400ms …
            }
        }
    }

    private void sleepWithJitter(long delayMs) {
        long jitter = (long) (Math.random() * delayMs * 0.3); // ±30% jitter
        try {
            Thread.sleep(delayMs + jitter);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
```

---

## Stage 3 — Virtual Threads (Java 21 upgrade)

### What the interviewer says

> *"How would you change this design to handle 100,000 concurrent notifications?"*

---

### Implementation — drop-in virtual thread upgrade

```java
// Replace the fixed thread pool with a virtual thread executor
// One virtual thread per task — JVM schedules them on platform threads automatically
this.workers = Executors.newVirtualThreadPerTaskExecutor();

// Remove the processLoop entirely — each notification gets its own virtual thread
public void dispatch(PaymentEvent event) {
    workers.submit(() -> {
        try {
            service.send(event); // blocking I/O here parks the virtual thread, not the OS thread
        } catch (Exception e) {
            log.error("Notification failed: {}", e.getMessage());
        }
    });
}
```

---

### Trade-off discussion to lead

> *"With a fixed thread pool of, say, 20 workers, 20 slow notification sends block all 20 threads and queue builds up. Virtual threads solve this — each send gets its own thread; blocking I/O parks the virtual thread without blocking an OS thread. We go from ~1,000 to potentially millions of concurrent operations with the same hardware. The caveat: the notification service itself (HTTP endpoint, database) becomes the bottleneck, and virtual threads don't help there — that requires backpressure or Reactive Streams semantics."*

---

---

# Quick Reference — Interview Phrases That Land Well

| Situation | What to say |
|---|---|
| Starting any problem | *"Before I code, let me confirm my understanding and check a few assumptions…"* |
| Choosing `synchronized` | *"I'll start with the simplest correct solution — synchronized — and we can discuss lock-free alternatives once it's working."* |
| Asked about performance | *"I'd profile before optimising. The bottleneck is rarely where you expect it."* |
| Concurrency trade-off | *"synchronized pins carrier threads in Java 21 virtual thread contexts — ReentrantLock is the safer choice there."* |
| Cross-service consistency | *"Two-phase commit is fragile at scale. I'd use a Saga with compensating transactions and an outbox pattern for reliable event publishing."* |
| Idempotency | *"The database unique constraint on the idempotency key is the true distributed lock — it works across all pods without coordination."* |
| Failure handling | *"Every external call needs a timeout, a retry budget, and a fallback — I think of these as mandatory, not optional."* |
| AI-generated code | *"I used autocomplete for the test scaffolding — let me walk you through each line to confirm I understand it."* |

---

*Good luck — speed, simplicity, and loud thinking are the three levers.*

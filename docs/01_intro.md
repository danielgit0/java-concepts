# FinTech Backend Interview Preparation Guide

---

# FinTech Mindset: What Interviewers Are Looking For

## Speed + Simplicity

Always start with:

1. Clarify requirements
2. State assumptions
3. Propose simplest working solution
4. Implement
5. Add tests
6. Discuss improvements and trade-offs

Bad:

> "I'll build a highly scalable distributed architecture."

Good:

> "I'll first implement the simplest correct solution, then discuss scaling and concurrency improvements."

---

# Coding Round Strategy

## Before Coding

Always verbalize:

### Understanding

"Let me restate the problem to ensure I understand correctly."

### Assumptions

"I'll assume X unless you want different behavior."

### Initial Design

"My first implementation will optimize for correctness and simplicity."

### Future Improvements

"After the basic solution works, we can discuss performance and concurrency."

---

# Testing Strategy

For every stage:

## Happy Path

Normal successful scenario.

## Edge Cases

- Empty input
- Null input
- Single element
- Duplicate values
- Large values

## Failure Cases

- Invalid request
- Missing entity
- Concurrent updates

Example:

```java
@Test
void shouldTransferMoney() {}

@Test
void shouldRejectInsufficientFunds() {}

@Test
void shouldHandleConcurrentTransfers() {}
```

---

# Java Concurrency

This is one of the highest-probability topics.

---

## Thread Safety

Question:

Can multiple threads access this object safely?

Not thread-safe:

```java
class Counter {
    int value = 0;

    void increment() {
        value++;
    }
}
```

Problem:

```java
value++
```

is actually:

```java
read
modify
write
```

Two threads can overwrite each other.

---

## synchronized

```java
class Counter {
    private int value;

    synchronized void increment() {
        value++;
    }
}
```

Pros:

- Simple
- Easy to reason about

Cons:

- Blocking
- Lower throughput

---

## AtomicInteger

```java
AtomicInteger counter = new AtomicInteger();

counter.incrementAndGet();
```

Pros:

- Lock-free
- Better performance

Cons:

- Only suitable for simple operations

---

## ConcurrentHashMap

Bad:

```java
HashMap<String, User>
```

Good:

```java
ConcurrentHashMap<String, User>
```

Useful methods:

```java
computeIfAbsent()
putIfAbsent()
```

---

## Race Conditions

Example:

Two transfers update same account.

Current balance:

100

Thread A:

Withdraw 80

Thread B:

Withdraw 50

Both read 100.

Final balance becomes incorrect.

Solutions:

- synchronized
- locks
- optimistic locking
- database transactions

---

## Deadlocks

Example:

Thread A:

```java
lock(accountA)
lock(accountB)
```

Thread B:

```java
lock(accountB)
lock(accountA)
```

Both wait forever.

Prevention:

Always acquire locks in consistent order.

---

## ReentrantLock

```java
Lock lock = new ReentrantLock();

lock.lock();

try {
   // work
}
finally {
   lock.unlock();
}
```

Advantages:

- timeout support
- fairness option
- more flexible

---

## Producer Consumer

Classic concurrency problem.

Use:

```java
BlockingQueue
```

```java
queue.put(item);

queue.take();
```

---

# Data Consistency

Critical for fintech systems.

---

## ACID

### Atomicity

All or nothing.

Money transfer:

```text
withdraw
deposit
```

Both succeed or neither.

---

### Consistency

Valid state before and after transaction.

Balance never becomes negative.

---

### Isolation

Concurrent transactions do not corrupt data.

---

### Durability

Committed data survives crashes.

---

# Database Isolation Levels

---

## Read Uncommitted

Can see uncommitted data.

Problem:

Dirty reads.

Rarely used.

---

## Read Committed

Most common.

Prevents dirty reads.

Still allows:

- non-repeatable reads

---

## Repeatable Read

Same query returns same result within transaction.

Prevents:

- dirty reads
- non-repeatable reads

---

## Serializable

Strongest isolation.

Behaves like sequential execution.

Highest consistency.

Lowest throughput.

---

# Optimistic vs Pessimistic Locking

---

## Optimistic Locking

Assume conflicts are rare.

```java
@Version
Long version;
```

Update succeeds only if version unchanged.

Pros:

- high throughput

Cons:

- retries required

Great for fintech balances.

---

## Pessimistic Locking

```sql
SELECT *
FROM account
FOR UPDATE;
```

Pros:

- strong consistency

Cons:

- blocking
- reduced scalability

---

# Distributed Systems

Very likely topic.

---

## CAP Theorem

You can only fully achieve two of:

- Consistency
- Availability
- Partition Tolerance

Network partitions are unavoidable.

Modern systems choose between:

CP or AP

---

## Eventual Consistency

Updates propagate asynchronously.

Example:

Account profile updates.

Not suitable for:

- balance calculations

Suitable for:

- analytics
- search indexes
- notifications

---

# Microservices

Why?

Large teams.

Independent deployment.

Independent scaling.

---

## Benefits

- team autonomy
- independent deployments
- fault isolation

---

## Problems

- network failures
- distributed transactions
- observability
- debugging complexity

---

# Domain Driven Design (DDD)

Common FinTech topic.

---

## Core Concepts

### Entity

Has identity.

```java
Account
User
Card
```

---

### Value Object

No identity.

```java
Money
Address
Email
```

Immutable.

---

### Aggregate

Consistency boundary.

Example:

```text
Account Aggregate

Account
 └── Transactions
```

All business rules enforced inside aggregate.

---

### Bounded Context

Different domains with separate models.

Examples:

```text
Payments
Cards
Loans
Crypto
```

---

# CQRS

Command Query Responsibility Segregation

Separate:

## Commands

Modify state.

```java
CreateAccount
TransferMoney
```

---

## Queries

Read state.

```java
GetBalance
GetTransactions
```

Benefits:

- independent scaling
- optimized reads

Trade-off:

- more complexity

---

# Event Driven Architecture

Extremely relevant at FinTech scale.

---

## Event

Something happened.

```java
MoneyTransferred
CardCreated
PaymentCompleted
```

---

## Flow

Payment Service

publishes:

```text
PaymentCompleted
```

Consumers:

- Notifications
- Analytics
- Fraud Detection

---

## Benefits

Loose coupling.

Independent scaling.

---

## Problems

Duplicate events.

Out-of-order events.

Need idempotency.

---

# Idempotency

Fintech favorite question.

---

Problem:

User retries transfer.

Without idempotency:

```text
Transfer €100
Transfer €100
```

Result:

€200 transferred.

Bad.

---

Solution:

Use idempotency key.

```text
Request-ID
```

Same request:

Return previous result.

No duplicate transfer.

---

# Reliability

---

## Retry Pattern

Retry transient failures.

Example:

```java
Retry 3 times
```

Use:

- exponential backoff
- jitter

Avoid retry storms.

---

## Circuit Breaker

Dependency failing.

Instead of continuously calling it:

Open circuit.

Reject immediately.

States:

- Closed
- Open
- Half-open

Popular library:

Resilience4j

---

## Timeout

Never wait forever.

Example:

```java
client.call(timeout=2s)
```

---

## Bulkhead Pattern

Prevent one dependency from consuming all resources.

Separate thread pools.

---

# Scalability

---

## Vertical Scaling

Bigger machine.

Easy.

Limited.

---

## Horizontal Scaling

More machines.

Preferred.

Requires:

- stateless services
- load balancing

---

## Load Balancer

Distributes requests.

Examples:

- NGINX
- HAProxy
- cloud load balancers

---

# Caching

---

## Why

Reduce latency.

Reduce database load.

---

## Common Cache

Redis.

---

## Cache Problems

### Cache Stampede

Many requests after expiration.

### Stale Data

Cache outdated value.

Solutions:

- TTL
- cache invalidation
- write-through

---

# Messaging Systems

Common distributed systems topic.

---

## Kafka

Good for:

- event streams
- analytics
- high throughput

Guarantees:

- ordering within partition

---

## RabbitMQ

Good for:

- task queues
- work distribution

---

# Designing a Money Transfer Service

Excellent practice question.

---

## Requirements

Transfer money between accounts.

Must:

- never lose money
- never create money
- support concurrency

---

## Simple Design

```text
Transfer API

Account Service

Database
```

Transaction:

```text
1. Lock source account
2. Verify balance
3. Withdraw
4. Deposit
5. Commit
```

---

## Scale Improvements

- optimistic locking
- idempotency keys
- event publishing
- audit log
- fraud checks
- retries
- monitoring

---

# Deployment & Production

---

## Blue-Green Deployment

Two environments:

Blue (current)

Green (new)

Switch traffic instantly.

Easy rollback.

---

## Canary Deployment

Small percentage:

```text
1%
5%
20%
100%
```

Safer.

Preferred for critical systems.

---

# Monitoring

Key metrics:

## RED

Request Rate

Error Rate

Duration

---

## Golden Signals

Latency

Traffic

Errors

Saturation

---

# Common FinTech Technical Conversation Questions

### Concurrency

- Difference between synchronized and AtomicInteger?
- How would you prevent race conditions in money transfers?
- What causes deadlocks?

### Databases

- Explain ACID.
- Isolation levels?
- Optimistic vs pessimistic locking?

### Architecture

- What is DDD?
- What problem does CQRS solve?
- When would you use event-driven architecture?

### Distributed Systems

- Explain CAP theorem.
- Eventual consistency?
- How would you design money transfer between services?

### Reliability

- What is idempotency?
- Circuit breaker?
- Retry strategy?

### Scalability

- How would you scale to 70+ million customers?
- How would you reduce database load?
- How would you handle traffic spikes?

---

# Final Interview Formula

For every question:

1. Clarify requirements
2. Start with simplest correct solution
3. Discuss complexity
4. Discuss concurrency
5. Discuss consistency
6. Discuss scalability
7. Discuss reliability
8. Discuss trade-offs

This structure closely matches how senior engineers reason about production systems at large fintech companies.

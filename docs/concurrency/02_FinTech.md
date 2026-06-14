# Senior+ Fintech Topics

These are the topics that often differentiate Senior engineers from Staff/Lead candidates in fintech interviews.

---

# Ledger Design

One of the most important fintech concepts.

A common mistake is treating an account balance as the source of truth.

Bad:

```sql
Account
-------
id
balance
```

Updating balances directly creates problems:

- race conditions
- difficult auditing
- difficult reconciliation
- impossible to reconstruct history

---

## Ledger-Based Approach

Instead:

```text
Account
Ledger Entries
```

Balance becomes a derived value.

Example:

```text
+1000 Salary
-200 Rent
-50 Subscription
```

Current balance:

```text
750
```

computed from entries.

---

## Double Entry Accounting

Every movement has two sides.

Example:

User transfers €100 to another user.

Ledger:

```text
User A  -100
User B  +100
```

Money is neither created nor destroyed.

Invariant:

```text
sum(all entries) = 0
```

This is how banks and fintechs typically think.

---

## Ledger Interview Discussion

Expect questions such as:

> How do you guarantee money is never lost?

Answer:

- immutable ledger
- transactional writes
- double-entry bookkeeping
- reconciliation processes

---

# Distributed Transactions

Easy in one database.

Hard across services.

---

## Example

Transfer flow:

```text
Payment Service
↓
Account Service
↓
Notification Service
↓
Fraud Service
```

What if Account Service succeeds but Notification Service fails?

---

## Two-Phase Commit (2PC)

Coordinator asks:

```text
Can you commit?
```

All services vote.

Then:

```text
Commit
```

or

```text
Rollback
```

---

### Problems

- blocking
- poor scalability
- poor fault tolerance

Rarely used in modern microservices.

---

# Saga Pattern

Preferred approach.

Break transaction into local transactions.

---

## Example

Travel Booking

Step 1:

```text
Reserve Flight
```

Step 2:

```text
Reserve Hotel
```

Step 3:

```text
Reserve Car
```

---

Failure:

Hotel reservation fails.

Compensating action:

```text
Cancel Flight Reservation
```

---

## Fintech Example

Transfer Flow:

```text
Reserve Funds
```

↓

```text
Perform Transfer
```

↓

```text
Send Confirmation
```

Failure:

Transfer fails.

Compensation:

```text
Release Reserved Funds
```

---

## Saga Types

### Choreography

Services react to events.

```text
OrderCreated
PaymentReserved
InventoryReserved
```

Pros:

- simple
- decentralized

Cons:

- difficult to understand
- difficult to debug

---

### Orchestration

Central coordinator.

```text
Saga Orchestrator
```

drives workflow.

Pros:

- easier control
- easier visibility

Cons:

- additional component

---

# Outbox Pattern

Very common interview topic.

---

## Problem

Consider:

```text
1. Save payment
2. Publish event
```

Database succeeds.

Kafka publish fails.

Now:

```text
Payment exists
Event does not
```

System becomes inconsistent.

---

## Solution

Write both into same transaction.

```sql
Payment
OutboxEvent
```

Commit together.

---

Worker process:

```text
Read Outbox
Publish Event
Mark Processed
```

Guarantee:

```text
If data exists,
event will eventually be published.
```

---

## Why FinTech Cares

Large event-driven systems frequently rely on:

- Kafka
- outbox pattern
- eventual consistency

---

# Exactly-Once Semantics

Popular interview trap.

---

## Reality

True exactly-once processing is extremely difficult.

What businesses usually need is:

```text
Effectively Once
```

processing.

---

## Example

Network timeout occurs.

Client retries.

Server receives:

```text
TransferRequest
TransferRequest
```

twice.

Without protection:

```text
Money moved twice.
```

---

## Solution

Idempotency key.

```text
request-id
```

Store:

```text
request-id
result
```

Duplicate requests return previous result.

---

# Idempotency Design

Typical API:

```http
POST /transfers

Idempotency-Key:
abc123
```

Server:

```text
Has key?
  Yes -> return previous response

  No -> execute transfer
```

---

## Common Fintech Rule

All payment operations should be idempotent.

---

# Kafka Delivery Guarantees

Important for senior backend roles.

---

## At Most Once

Message may be lost.

Never duplicated.

```text
0 or 1 delivery
```

---

## At Least Once

Message may be duplicated.

Never lost.

```text
1 or more deliveries
```

Most common.

---

## Exactly Once

Kafka provides EOS support.

Still requires careful application design.

Most systems still implement idempotency.

---

# Ordering Guarantees

Question:

> Does Kafka guarantee ordering?

Answer:

Within a partition only.

Example:

Partition A:

```text
Event1
Event2
Event3
```

Ordering preserved.

Across partitions:

No guarantee.

---

# Event Versioning

Common production challenge.

---

Version 1:

```json
{
  "userId": "123"
}
```

Version 2:

```json
{
  "userId": "123",
  "country": "AT"
}
```

Consumers may still expect V1.

---

Strategies:

- backward compatibility
- schema registry
- Avro / Protobuf

---

# Reconciliation

Critical fintech topic.

---

Question:

> How do you know your balances are correct?

Answer:

Regular reconciliation.

Compare:

```text
Internal Ledger
```

against:

```text
Bank Records
Payment Processor
Card Networks
```

---

Example

Expected:

```text
€1,000,000
```

Actual:

```text
€999,900
```

Need investigation.

---

# Eventual Consistency in Payments

Question:

> Should payment balances be eventually consistent?

Generally:

No.

Balances usually require strong consistency.

---

Good candidates often distinguish:

Strong consistency:

```text
Balance
Ledger
Transfers
```

Eventual consistency:

```text
Notifications
Analytics
Reporting
Search
```

---

# Handling Duplicate Events

Always assume duplicates exist.

Consumer example:

```text
PaymentCompleted
```

received twice.

Solution:

Store processed event IDs.

```sql
processed_events
```

Skip duplicates.

---

# Designing a High-Scale Payment Service

A common Staff-level discussion.

---

## Core Requirements

- no money loss
- no duplicate transfers
- auditable
- scalable
- resilient

---

## Architecture

```text
API Gateway

Payment Service

Ledger Service

Kafka

Notification Service

Fraud Service

Reporting Service
```

---

## Important Design Decisions

### Idempotency Keys

Prevent duplicate requests.

---

### Ledger

Immutable source of truth.

---

### Outbox

Reliable event publication.

---

### Kafka

Asynchronous processing.

---

### Retry Strategy

Transient failures only.

---

### Monitoring

Track:

- failed payments
- reconciliation mismatches
- duplicate requests
- event lag

---

# Trade-Off Discussions Interviewers Love

## Strong Consistency vs Availability

Question:

Would you rather reject payments or risk inconsistent balances?

For financial systems:

```text
Consistency > Availability
```

often wins.

---

## Synchronous vs Asynchronous

Synchronous:

Pros:

- immediate result

Cons:

- tighter coupling

---

Asynchronous:

Pros:

- scalable
- resilient

Cons:

- eventual consistency

---

## Monolith vs Microservices

Monolith:

Pros:

- simpler
- easier transactions

Cons:

- harder scaling

---

Microservices:

Pros:

- independent scaling
- team autonomy

Cons:

- distributed systems complexity

---

# Senior-Level Answer Pattern

For architecture questions:

1. Define requirements
2. Define consistency requirements
3. Define failure scenarios
4. Discuss data ownership
5. Discuss scaling strategy
6. Discuss observability
7. Discuss trade-offs
8. Explain why you chose the final design

Interviewers often care more about the trade-off discussion than the final architecture itself.

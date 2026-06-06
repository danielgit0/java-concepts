package org.example.coding_problems.problem01;

import java.time.Instant;

public record BalanceEvent(
    String transactionId,
    EventType type,
    long balanceBefore,
    long amount,
    long balanceAfter,
    Instant occurredAt) {
  public enum EventType {
    CREDIT,
    DEBIT
  }
}

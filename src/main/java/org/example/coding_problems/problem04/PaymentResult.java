package org.example.coding_problems.problem04;

import java.time.Instant;

public record PaymentResult(String paymentId, Status status, Instant processedAt) {
  public enum Status {
    SUCCESS,
    FAILED,
    INSUFFICIENT_FUNDS
  }
}

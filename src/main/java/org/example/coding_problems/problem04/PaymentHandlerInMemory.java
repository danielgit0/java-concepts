package org.example.coding_problems.problem04;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.example.coding_problems.problem04.PaymentResult.Status;

public class PaymentHandlerInMemory implements PaymentHandler {

  private final int delayMs;
  private final Map<String, Long> balances =
      new ConcurrentHashMap<>(
          Map.of(
              "alice", 10_000L,
              "bob", 5_000L));

  public PaymentHandlerInMemory(int delayMs) {
    this.delayMs = delayMs;
  }

  @Override
  public PaymentResult execute(PaymentRequest request) {
    return execute(request.from(), request.to(), request.amount());
  }

  @Override
  public PaymentResult execute(PaymentRequestWithoutIdempKey request) {
    return execute(request.from(), request.to(), request.amount());
  }

  private PaymentResult execute(final String from, final String to, final long amount) {
    Long fromBalance = balances.get(from);

    if (fromBalance == null || fromBalance < amount) {
      return new PaymentResult(
          UUID.randomUUID().toString(), Status.INSUFFICIENT_FUNDS, Instant.now());
    }

    try {
      Thread.sleep(delayMs);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }

    balances.merge(from, amount, Long::sum);
    balances.merge(to, amount, Long::sum);
    balances.computeIfPresent(from, (k, v) -> v - amount);
    balances.computeIfPresent(to, (k, v) -> v + amount);

    return new PaymentResult(UUID.randomUUID().toString(), Status.SUCCESS, Instant.now());
  }
}

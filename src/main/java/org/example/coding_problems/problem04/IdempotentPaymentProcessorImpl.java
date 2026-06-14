package org.example.coding_problems.problem04;

import java.util.concurrent.ConcurrentHashMap;

public class IdempotentPaymentProcessorImpl implements IdempotentPaymentProcessor {
  private final PaymentHandler handler;
  private final ConcurrentHashMap<String, PaymentResult> store = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, Object> locks = new ConcurrentHashMap<>();

  public IdempotentPaymentProcessorImpl(PaymentHandler handler) {
    this.handler = handler;
  }

  @Override
  public PaymentResult process(PaymentRequest request) {
    return store.computeIfAbsent(request.key(), key -> handler.execute(request));
  }

  @Override
  public PaymentResult process(PaymentRequestWithoutIdempKey request) {
    return store.computeIfAbsent(String.valueOf(request.hashCode()), _ -> handler.execute(request));
  }

  @Override
  public PaymentResult processWithDeduplication(PaymentRequest request) {
    String key = request.key();

    PaymentResult existing = store.get(key);
    if (existing != null) {
      return existing;
    }

    Object lock = locks.computeIfAbsent(key, _ -> new Object());
    synchronized (lock) {
      return store.computeIfAbsent(key, k -> handler.execute(request));
    }
  }
}

package org.example.coding_problems.problem04;

public interface IdempotentPaymentProcessor {
  PaymentResult process(PaymentRequest request);

  PaymentResult process(PaymentRequestWithoutIdempKey request);

  PaymentResult processWithDeduplication(PaymentRequest request);
}

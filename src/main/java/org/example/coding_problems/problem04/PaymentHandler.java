package org.example.coding_problems.problem04;

public interface PaymentHandler {
  PaymentResult execute(PaymentRequest request);

  PaymentResult execute(PaymentRequestWithoutIdempKey request);
}

package org.example.coding_problems.problem04;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.example.coding_problems.problem04.PaymentResult.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class Problem04Test {

  private IdempotentPaymentProcessor processor;

  @BeforeEach
  void setUp() {
    processor = new IdempotentPaymentProcessorImpl(new PaymentHandlerInMemory(0));
  }

  @Test
  void sameIdempotencyKey_returnsSameResult() {
    PaymentRequest request = new PaymentRequest("idem-key-1", "alice", "bob", 100L);
    PaymentResult response1 = processor.process(request);
    PaymentResult response2 = processor.process(request);

    assertThat(response1).isEqualTo(response2);
  }

  @Test
  void differentKeysAreProcessed() {
    PaymentRequest request1 = new PaymentRequest("idem-key-1", "alice", "bob", 100L);
    PaymentRequest request2 = new PaymentRequest("idem-key-2", "alice", "bob", 100L);
    PaymentResult response1 = processor.process(request1);
    PaymentResult response2 = processor.process(request2);

    assertThat(response1).isNotEqualTo(response2);
  }

  @Test
  void failedPayment_isNotReplayed_asSuccess() {
    PaymentRequest req = new PaymentRequest("key-insufficient", "alice", "bob", 10_001L);

    PaymentResult first = processor.process(req);
    PaymentResult second = processor.process(req);

    assertThat(first.status()).isEqualTo(Status.INSUFFICIENT_FUNDS);
    assertThat(second.status()).isEqualTo(Status.INSUFFICIENT_FUNDS);
  }

  @Test
  void serverSideIdempKey_returnsSameResult() {
    PaymentRequestWithoutIdempKey request = new PaymentRequestWithoutIdempKey("alice", "bob", 100L);

    IdempotentPaymentProcessor idempotentPaymentProcessor =
        new IdempotentPaymentProcessorImpl(new PaymentHandlerInMemory(0));

    PaymentResult response1 = idempotentPaymentProcessor.process(request);
    PaymentResult response2 = idempotentPaymentProcessor.process(request);

    assertThat(response1).isEqualTo(response2);
  }

  @Test
  void concurrentVirtualThreads_sameKey_handlerCalledExactlyOnce() throws InterruptedException {

    IdempotentPaymentProcessor idempotentPaymentProcessor =
        new IdempotentPaymentProcessorImpl(new PaymentHandlerInMemory(500));

    PaymentRequest request = new PaymentRequest("SHARED_KEY", "alice", "bob", 500L);

    int threads = 100;
    CountDownLatch latch = new CountDownLatch(threads);

    List<PaymentResult> results = new CopyOnWriteArrayList<>();
    List<Throwable> errors = new CopyOnWriteArrayList<>();

    try (ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < threads; i++) {
        executor.submit(
            () -> {
              try {
                results.add(idempotentPaymentProcessor.processWithDeduplication(request));
              } catch (Throwable t) {
                errors.add(t);
              } finally {
                latch.countDown();
              }
            });
      }

      assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
      assertThat(errors).isEmpty();
    }

    assertThat(results).hasSize(threads);

    PaymentResult canonical = results.get(0);
    assertThat(results)
        .as("all threads must receive the identical PaymentResult")
        .allSatisfy(
            result -> {
              assertThat(result.paymentId()).isEqualTo(canonical.paymentId());
              assertThat(result.status()).isEqualTo(canonical.status());
              assertThat(result.processedAt()).isEqualTo(canonical.processedAt());
            });
  }
}

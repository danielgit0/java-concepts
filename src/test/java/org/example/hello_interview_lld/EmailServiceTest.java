package org.example.hello_interview_lld;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;

class EmailServiceTest {

  @Test
  void testConcurrencyBackpressureWithVirtualThreads() throws InterruptedException {
    EmailService service = new EmailService();
    int queueCapacity = 10;

    for (int i = 0; i < queueCapacity; i++) {
      service.publishEmail(new Email(String.valueOf(i), "from", "to", "subject", "body"));
    }

    AtomicBoolean publisherFinished = new AtomicBoolean(false);
    CountDownLatch publisherStarted = new CountDownLatch(1);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(
          () -> {
            publisherStarted.countDown();
            service.publishEmail(new Email("11", "from11", "to11", "subject11", "body11"));
            publisherFinished.set(true);
          });

      assertTrue(publisherStarted.await(1, TimeUnit.SECONDS));

      Thread.sleep(200);

      assertFalse(publisherFinished.get(), "Publisher should be blocked by backpressure");

      service.consume();
    }

    assertTrue(publisherFinished.get(), "Publisher should finish after pool space opens up");
  }
}

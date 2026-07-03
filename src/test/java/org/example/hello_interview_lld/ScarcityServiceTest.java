package org.example.hello_interview_lld;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ScarcityServiceTest {

  @Test
  void testInitializationAndBasicExecution() throws Exception {
    ScarcityService service = new ScarcityService();
    assertDoesNotThrow(service::helloSemaphore);
    assertDoesNotThrow(service::helloBlockingQueue);
  }

  @Test
  void testSemaphoreThrottling() throws InterruptedException {
    ScarcityService service = new ScarcityService();
    int totalThreads = 10;

    CountDownLatch latch = new CountDownLatch(totalThreads);

    AtomicInteger activeThreads = new AtomicInteger(0);
    AtomicInteger maxConcurrentThreads = new AtomicInteger(0);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < totalThreads; i++) {
        executor.submit(
            () -> {
              try {
                service.permits.acquire();

                int current = activeThreads.incrementAndGet();
                maxConcurrentThreads.accumulateAndGet(current, Math::max);

                Thread.sleep(50);

                activeThreads.decrementAndGet();
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                service.permits.release();
                latch.countDown();
              }
            });
      }
    }

    latch.await();

    assertTrue(maxConcurrentThreads.get() <= 5);
  }
}

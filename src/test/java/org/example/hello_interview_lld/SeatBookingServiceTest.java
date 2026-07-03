package org.example.hello_interview_lld;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

public class SeatBookingServiceTest {

  @Test
  void seatIsBooked() {
    SeatBookingService seatBookingService = new SeatBookingService();

    assertThat(seatBookingService.bookSeat("S01", "O01")).isTrue();
  }

  @Test
  void canNotBookUnavailableSeat() {
    SeatBookingService seatBookingService = new SeatBookingService();

    assertThat(seatBookingService.bookSeat("S01", "O01")).isTrue();
    assertThat(seatBookingService.bookSeat("S01", "O02")).isFalse();
  }

  @Test
  void concurrentReservationsAreConsistent() throws InterruptedException {
    SeatBookingService seatBookingService = new SeatBookingService();
    String targetSeatId = "A1";
    int threads = 100;

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch finishLatch = new CountDownLatch(threads);
    AtomicInteger successfulBookings = new AtomicInteger(0);

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < threads; i++) {
        final String occupantId = "User-" + i;
        executor.submit(
            () -> {
              try {
                startLatch.await();
                boolean success = seatBookingService.bookSeatSynchronized(targetSeatId, occupantId);
                if (success) {
                  successfulBookings.incrementAndGet();
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              } finally {
                finishLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      finishLatch.await();
    }

    assertEquals(1, successfulBookings.get(), "Only one thread should successfully book the seat");
  }
}

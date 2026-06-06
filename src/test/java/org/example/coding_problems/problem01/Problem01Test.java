package org.example.coding_problems.problem01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.example.coding_problems.problem01.BalanceEvent.EventType;
import org.junit.jupiter.api.Test;

public class Problem01Test {

  @Test
  void credit_increasesBalance() {
    Account account = new Account("AT43", 1000L);
    account.credit(500L);
    assertThat(account.getBalance()).isEqualTo(1500L);
  }

  @Test
  void debit_reducesBalance() {
    Account account = new Account("AT43", 1000L);
    account.debit(400L);
    assertThat(account.getBalance()).isEqualTo(600L);
  }

  @Test
  void debit_throwsInsufficientFunds() {
    Account account = new Account("AT43", 1000L);

    assertThatThrownBy(() -> account.debit(1001L)).isInstanceOf(InsufficientFundsException.class);
  }

  @Test
  void credit_rejectsNegativeAmmount() {
    Account account = new Account("AT43", 1000L);
    assertThatThrownBy(() -> account.credit(-10L)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void concurrentDebits_neverProduceNegativeBalance() throws InterruptedException {
    Account account = new Account("AT43", 10_000L);
    ExecutorService executor = Executors.newFixedThreadPool(50);
    CountDownLatch latch = new CountDownLatch(200);

    for (int i = 0; i < 200; i++) {
      executor.submit(
          () -> {
            try {
              //          println(Thread.currentThread());
              account.debit(100L);
            } catch (InsufficientFundsException ignored) {
            } finally {
              latch.countDown();
            }
          });
    }

    latch.await(5, TimeUnit.SECONDS);
    assertThat(account.getBalance()).isGreaterThanOrEqualTo(0L);
  }

  @Test
  void synchronized_totalMoneyIsConserved() {
    AccountSynchronized account = new AccountSynchronized("AT43", 1_000_000L);

    int threads = 100;

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < threads; i++) {
        executor.submit(
            () -> {
              for (int j = 0; j < 10_000; j++) {
                account.credit(1L);
                account.debit(1L);
              }
            });
      }
    }

    assertThat(account.getBalance()).isEqualTo(1_000_000L);
  }

  @Test
  void atomic_totalMoneyIsConserved() {
    AccountAtomic account = new AccountAtomic("AT43", 1_000_000L);

    int threads = 100;

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < threads; i++) {
        executor.submit(
            () -> {
              for (int j = 0; j < 10_000; j++) {
                account.credit(1);
                account.debit(1);
              }
            });
      }
    }

    assertThat(account.getBalance()).isEqualTo(1_000_000L);
  }

  @Test
  void atomicArithmetic_failsOnOverflow() {
    AccountAtomicArithmetic account = new AccountAtomicArithmetic("AT43", 1L);

    assertThatThrownBy(() -> account.credit(Long.MAX_VALUE))
        .isInstanceOf(ArithmeticException.class);
  }

  @Test
  void atomicArithmetic_negativeOverflowIsNotPossible() {
    AccountAtomicArithmetic account = new AccountAtomicArithmetic("AT43", 1L);

    assertThatThrownBy(() -> account.debit(Long.MAX_VALUE))
        .isInstanceOf(InsufficientFundsException.class);
    assertThatThrownBy(() -> account.debit(Long.MIN_VALUE))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void eventsAreCreatedAndAreConsistent() {
    AccountAtomicArithmeticWithEvents account =
        new AccountAtomicArithmeticWithEvents("AT43", 1_000L);

    int threads = 100;

    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < threads; i++) {
        executor.submit(
            () -> {
              for (int j = 0; j < 100; j++) {
                account.credit(1);
                account.debit(1);
              }
            });
      }
    }

    final List<BalanceEvent> events = account.drainEvents();
    assertThat(events.size()).isEqualTo(20_000);
    assertThat(events.stream().filter(e -> e.type().equals(EventType.CREDIT)).count())
        .isEqualTo(10_000);
    assertThat(events.stream().filter(e -> e.type().equals(EventType.DEBIT)).count())
        .isEqualTo(10_000);

    //    println(events.subList(0, 10));
  }
}

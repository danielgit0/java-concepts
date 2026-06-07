package org.example.coding_problems.problem02;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class Problem02Test {

  private TransferService service;
  private Account alice;
  private Account bob;

  @BeforeEach
  void setUp() {
    service = new TransferServiceImpl();
    alice = new Account("alice", 1000L);
    bob = new Account("bob", 500L);
  }

  @Test
  void transfer_movesMoneyBetweenAccounts() {
    service.transfer(alice, bob, 300L);
    assertThat(alice.getBalance()).isEqualTo(700L);
    assertThat(bob.getBalance()).isEqualTo(800L);
  }

  @Test
  void transfer_failsWhenInsufficientFunds() {
    assertThatThrownBy(() -> service.transfer(alice, bob, 2000L))
        .isInstanceOf(InsufficientFundsException.class);

    assertThat(alice.getBalance()).isEqualTo(1000L);
    assertThat(bob.getBalance()).isEqualTo(500L);
  }

  @Test
  void transfer_toSameAccount_throws() {
    assertThatThrownBy(() -> service.transfer(alice, alice, 100L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void negativeAmountsAreIllegal() {
    assertThatThrownBy(() -> service.transfer(alice, bob, -1L))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void negativeBalancesAreNotAllowed() {
    assertThatThrownBy(() -> service.transfer(alice, bob, 1001L))
        .isInstanceOf(InsufficientFundsException.class);
  }

  @Test
  void concurrentTransfers_shouldNotDeadlock_andOverallBalanceIsInvariant()
      throws InterruptedException {
    int threads = 100;
    CountDownLatch latch = new CountDownLatch(threads);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < threads; i++) {
        executor.submit(
            () -> {
              try {
                service.transfer(alice, bob, 100L);
                service.transfer(bob, alice, 100L);
              } finally {
                latch.countDown();
                //                        println("Latch countdown:
                // %s".formatted(latch.getCount()));
              }
            });
      }
    }

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(alice.getBalance() + bob.getBalance()).isEqualTo(1500L);
  }

  @Test
  void concurrentTransfers_shouldNotDeadlock_andOverallBalanceIsInvariant_reentrantLock()
      throws InterruptedException {
    TransferService transferService = new TransferServiceImpl();
    AccountReentrantLock alice = new AccountReentrantLock("alice", 1000L);
    AccountReentrantLock bob = new AccountReentrantLock("bob", 500L);

    int threads = 100;
    CountDownLatch latch = new CountDownLatch(threads);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < threads; i++) {
        executor.submit(
            () -> {
              try {
                transferService.transfer(alice, bob, 100L);
                transferService.transfer(bob, alice, 100L);
              } finally {
                latch.countDown();
                // println("Latch countdown: %s".formatted(latch.getCount()));
              }
            });
      }
    }

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(alice.getBalance() + bob.getBalance()).isEqualTo(1500L);
  }

  @Test
  void concurrentTransfers_shouldNotDeadlock_andOverallBalanceIsInvariant_reentrantTryLock()
      throws InterruptedException {
    TransferService transferService = new TransferServiceImpl();
    AccountReentrantLock alice = new AccountReentrantLock("alice", 1000L);
    AccountReentrantLock bob = new AccountReentrantLock("bob", 500L);

    int threads = 100;
    CountDownLatch latch = new CountDownLatch(threads);
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      for (int i = 0; i < threads; i++) {
        executor.submit(
            () -> {
              try {
                transferService.transferTryLock(alice, bob, 100L);
                transferService.transferTryLock(bob, alice, 100L);
              } finally {
                latch.countDown();
                // println("Latch countdown: %s".formatted(latch.getCount()));
              }
            });
      }
    }

    assertThat(latch.await(10, TimeUnit.SECONDS)).isTrue();
    assertThat(alice.getBalance() + bob.getBalance()).isEqualTo(1500L);
  }
}

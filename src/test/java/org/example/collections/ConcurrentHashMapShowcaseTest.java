package org.example.collections;

import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class ConcurrentHashMapShowcaseTest {

  private static final int TOTAL_TASKS = 1000;
  private static final int THREAD_POOL_SIZE = 64;
  private static final int TOTAL_USERS = 10;
  private static final int TRANSACTION_AMOUNT = 100;
  private static final long EXPECTED_BALANCE_PER_USER =
      (TOTAL_TASKS / TOTAL_USERS) * TRANSACTION_AMOUNT;
  private static final long EXPECTED_TOTAL_BALANCE = EXPECTED_BALANCE_PER_USER * TOTAL_USERS;

  @Test
  public void testConcurrentHashMap() throws InterruptedException {
    System.out.println("\n--- CONCURRENT HASHMAP EXAMPLE ---");

    // PROBLEM: 100 threads updating the same balance cache simultaneously.
    // Using HashMap would cause ConcurrentModificationException or corrupted data.

    // SOLUTION: ConcurrentHashMap for thread-safe operations
    ConcurrentHashMap<String, AtomicLong> balanceCache = new ConcurrentHashMap<>();

    // 100 threads updating balances
    ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch latch = new CountDownLatch(TOTAL_TASKS);

    for (int i = 0; i < TOTAL_TASKS; i++) {
      final String userId = "user_" + (i % TOTAL_USERS);
      executor.submit(
          () -> {
            try {
              startGate.await(); // Synchronize thread takeoff to maximize collision risk
              Thread.sleep(1);
              balanceCache
                  .computeIfAbsent(userId, k -> new AtomicLong(0))
                  .addAndGet(TRANSACTION_AMOUNT);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              latch.countDown();
            }
          });
    }

    startGate.countDown(); // Unleash all pool threads simultaneously
    latch.await();
    executor.shutdown();

    System.out.println("Final balances (each user should have 10000): " + balanceCache);

    Assertions.assertEquals(TOTAL_USERS, balanceCache.size(), "Should have exactly 10 users");
    balanceCache.forEach(
        (user, balance) ->
            Assertions.assertEquals(
                EXPECTED_BALANCE_PER_USER, balance.get(), "Thread-safe Map failed for " + user));
    Assertions.assertEquals(
        EXPECTED_TOTAL_BALANCE,
        balanceCache.values().stream().map(AtomicLong::get).reduce(0L, Long::sum),
        "The total balance should be: " + EXPECTED_TOTAL_BALANCE);
  }

  @Test
  public void testUnreliableHashMap() throws InterruptedException {
    System.out.println("\n--- UNRELIABLE HASHMAP EXAMPLE ---");

    HashMap<String, Long> balanceCache = new HashMap<>();

    ExecutorService executor = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
    CountDownLatch startGate = new CountDownLatch(1);
    CountDownLatch latch = new CountDownLatch(TOTAL_TASKS);

    for (int i = 0; i < TOTAL_TASKS; i++) {
      final String userId = "user_" + (i % TOTAL_USERS);
      executor.submit(
          () -> {
            try {
              startGate.await();
              Long currentBalance = balanceCache.get(userId);
              Thread.sleep(1);
              if (currentBalance == null) {
                currentBalance = 0L;
              }

              balanceCache.put(userId, currentBalance + TRANSACTION_AMOUNT);
            } catch (InterruptedException e) {
              Thread.currentThread().interrupt();
            } finally {
              latch.countDown();
            }
          });
    }

    startGate.countDown();
    latch.await();
    executor.shutdown();

    System.out.println("Final balances (each user should have 10000): " + balanceCache);

    Assertions.assertEquals(
        TOTAL_USERS, balanceCache.size(), "Should have exactly 10 users. Could fail sometimes.");
    Assertions.assertTrue(
        balanceCache.values().stream()
            .anyMatch(balance -> !balance.equals(EXPECTED_BALANCE_PER_USER)),
        "Test failed because the map was unexpectedly accurate! No data loss occurred.");
    Assertions.assertNotEquals(
        EXPECTED_TOTAL_BALANCE,
        balanceCache.values().stream().reduce(0L, Long::sum),
        "The total balance should not be: " + EXPECTED_TOTAL_BALANCE);
  }
}

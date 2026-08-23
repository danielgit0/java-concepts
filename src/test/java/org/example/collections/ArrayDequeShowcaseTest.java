package org.example.collections;

import java.util.ArrayDeque;
import java.util.Deque;
import org.junit.jupiter.api.Test;

public class ArrayDequeShowcaseTest {

  @Test
  public void testArrayDeque() {
    System.out.println("\n--- ARRAYDEQUE EXAMPLE ---");

    // PROBLEM: Transaction processor needs to process from front,
    // but also add high-priority transactions to front.
    // LinkedList is slow and memory-heavy.

    // SOLUTION: ArrayDeque for O(1) operations at both ends
    Deque<String> transactionQueue = new ArrayDeque<>();

    transactionQueue.addLast("Regular TX1");
    transactionQueue.addLast("Regular TX2");
    transactionQueue.addLast("Regular TX3");

    // Add priority transaction to front
    transactionQueue.addFirst("PRIORITY: Fraud Check");

    System.out.println("Processing order:");
    while (!transactionQueue.isEmpty()) {
      System.out.println("  " + transactionQueue.pollFirst());
    }
  }
}

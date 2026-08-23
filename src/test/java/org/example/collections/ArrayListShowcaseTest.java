package org.example.collections;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

public class ArrayListShowcaseTest {

  @Test
  public void testArrayList() {
    System.out.println("\n--- ARRAYLIST EXAMPLE ---");

    // PROBLEM: Need random access to transaction by index for batch processing.
    // LinkedList would be O(n) for random access.

    // SOLUTION: ArrayList for O(1) indexed access
    List<String> transactionList = new ArrayList<>(1000); // Pre-size for performance

    for (int i = 0; i < 1000; i++) {
      transactionList.add("TX" + String.format("%05d", i));
    }

    String firstTx = transactionList.get(0);
    String middleTx = transactionList.get(500);
    String lastTx = transactionList.get(999);

    System.out.println("First: " + firstTx + ", Middle: " + middleTx + ", Last: " + lastTx);

    // Batch processing with index
    List<String> first100 = transactionList.subList(0, 100);
    System.out.println("First 100 transactions: " + first100.size() + " items");
  }
}

package org.example.collections;

import java.util.EnumMap;
import org.junit.jupiter.api.Test;

public class EnumMapShowcaseTest {

  enum TransactionStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED
  }

  @Test
  public void testEnumMap() {
    System.out.println("\n--- ENUMMAP EXAMPLE ---");

    /* PROBLEM: Need to track count of transactions by status.

       HashMap would work but is inefficient and wastes memory.
          * the keys belong to a fixed, known size of elements
          * Uses an internal array of "buckets," where each bucket contains node objects (linked lists or tree nodes). It has to store hash codes, keys, and values, and it often creates extra wrapper objects.
       EnumMap
          * does not compute hash codes or handle collisions, ecause enum constants have a sequential internal index,
            EnumMap finds the value using a direct array index lookup. This makes it an O(1) operation that executes much faster at the CPU level.
    */

    // SOLUTION: EnumMap
    EnumMap<TransactionStatus, Integer> statusCounts = new EnumMap<>(TransactionStatus.class);

    // Initialize all statuses to 0
    for (TransactionStatus status : TransactionStatus.values()) {
      statusCounts.put(status, 0);
    }

    // Simulate transaction status updates
    TransactionStatus[] updates = {
      TransactionStatus.PENDING, TransactionStatus.PROCESSING,
      TransactionStatus.COMPLETED, TransactionStatus.PENDING,
      TransactionStatus.COMPLETED, TransactionStatus.FAILED
    };

    for (TransactionStatus status : updates) {
      statusCounts.merge(status, 1, Integer::sum);
    }

    System.out.println("Transaction counts by status: " + statusCounts);
    // Output: {PENDING=2, PROCESSING=1, COMPLETED=2, FAILED=1}
  }
}

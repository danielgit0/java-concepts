package org.example.collections;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

public class HashMapShowcaseTest {

  @Test
  public void testHashMap() {
    IO.println("\n--- HASHMAP EXAMPLE ---");

    // PROBLEM: You're processing 1M transactions and need to aggregate
    // total spending per user. Using List would require O(n²) lookups.

    // SOLUTION: HashMap for O(1) lookups
    Map<String, Double> userSpending = new HashMap<>();

    // Simulate transaction processing
    String[] users = {"alice", "bob", "charlie", "alice", "bob"};
    double[] amounts = {100.50, 50.25, 75.00, 200.00, 30.00};

    for (int i = 0; i < users.length; i++) {
      userSpending.merge(users[i], amounts[i], Double::sum);
    }

    assertThat(userSpending)
        .hasSize(3)
        .containsEntry("alice", 300.5)
        .containsEntry("bob", 80.25)
        .containsEntry("charlie", 75.0);
  }
}

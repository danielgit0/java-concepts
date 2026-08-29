package org.example.collections;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.Test;

public class CopyOnWriteArrayListShowcaseTest {

  @Test
  public void testCopyOnWriteArrayList() {
    System.out.println("\n--- COPYONWRITEARRAYLIST EXAMPLE WITH VIRTUAL THREADS ---");

    // PROBLEM: Many threads reading configuration rules, few updates.
    // Regular ArrayList would need manual synchronization for all reads.

    // SOLUTION: CopyOnWriteArrayList for read-heavy scenarios
    List<String> processingRules = new CopyOnWriteArrayList<>();

    // Initial rules (happens rarely)
    processingRules.add("Rule1: Validate amount");
    processingRules.add("Rule2: Check fraud");
    processingRules.add("Rule3: Apply fee");

    // 10 reader threads
    ExecutorService readers = Executors.newFixedThreadPool(10);
    for (int i = 0; i < 10; i++) {
      readers.submit(
          () -> {
            for (int j = 0; j < 100; j++) {
              // Safe iteration without locks, even during updates
              for (String rule : processingRules) {
                // Process rule
                System.out.println(
                    " Thread " + Thread.currentThread() + " processing rule " + rule);
                rule.hashCode(); // Simulate work
              }
            }
          });
    }

    // Occasional updates (happens rarely)
    processingRules.add("Rule4: Log transaction");
    processingRules.remove("Rule2: Check fraud");

    readers.shutdown();
    System.out.println("Final rules: " + processingRules);
    // Note: No ConcurrentModificationException despite concurrent reads/writes
  }

  @Test
  public void testCopyOnWriteArrayListWithVirtualThreads() {
    System.out.println("\n--- COPYONWRITEARRAYLIST EXAMPLE ---");

    // PROBLEM: Many threads reading configuration rules, few updates.
    // Regular ArrayList would need manual synchronization for all reads.

    // SOLUTION: CopyOnWriteArrayList for read-heavy scenarios
    List<String> processingRules = new CopyOnWriteArrayList<>();

    // Initial rules (happens rarely)
    processingRules.add("Rule1: Validate amount");
    processingRules.add("Rule2: Check fraud");
    processingRules.add("Rule3: Apply fee");

    // 10 reader threads
    ExecutorService readers = Executors.newVirtualThreadPerTaskExecutor();
    for (int i = 0; i < 10; i++) {
      readers.submit(
          () -> {
            for (int j = 0; j < 100; j++) {
              // Safe iteration without locks, even during updates
              for (String rule : processingRules) {
                // Process rule
                System.out.println(
                    " Thread " + Thread.currentThread() + " processing rule " + rule);
                rule.hashCode(); // Simulate work
              }
            }
          });
    }

    // Occasional updates (happens rarely)
    processingRules.add("Rule4: Log transaction");
    processingRules.remove("Rule2: Check fraud");

    readers.shutdown();
    System.out.println("Final rules: " + processingRules);
    // Note: No ConcurrentModificationException despite concurrent reads/writes
  }
}

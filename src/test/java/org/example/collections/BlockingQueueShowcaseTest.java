package org.example.collections;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import org.junit.jupiter.api.Test;

public class BlockingQueueShowcaseTest {

  @Test
  public void testBlockingQueue() throws InterruptedException {
    System.out.println("\n--- BLOCKINGQUEUE EXAMPLE ---");

    // PROBLEM: Producer-consumer pattern with different speeds.
    // Producer is faster than consumer - need backpressure to prevent OOM.

    // SOLUTION: BlockingQueue with bounded capacity
    BlockingQueue<String> queue = new ArrayBlockingQueue<>(5);

    // Producer thread
    Thread producer =
        new Thread(
            () -> {
              try {
                for (int i = 1; i <= 10; i++) {
                  String transaction = "TX" + i;
                  queue.put(transaction); // Blocks if queue is full
                  System.out.println(
                      "Produced: " + transaction + " (queue size: " + queue.size() + ")");
                  Thread.sleep(100); // Produce every 100ms
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            });

    // Consumer thread (slower)
    Thread consumer =
        new Thread(
            () -> {
              try {
                while (true) {
                  String transaction = queue.take(); // Blocks if queue empty
                  System.out.println("  Consumed: " + transaction);
                  Thread.sleep(300); // Process takes 300ms (slower than producer)
                }
              } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
              }
            });

    producer.start();
    consumer.start();

    Thread.sleep(4000); // Let it run for 4 seconds
    producer.interrupt();
    consumer.interrupt();
    // Queue size will fluctuate but never exceed 5 due to backpressure
  }
}

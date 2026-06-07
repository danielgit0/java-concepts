package org.example.virtual_threads;

import static java.lang.IO.println;

import java.util.concurrent.Executors;
import java.util.concurrent.StructuredTaskScope;
import java.util.concurrent.StructuredTaskScope.Joiner;
import org.junit.jupiter.api.Test;

public class VirtualThreadsShowcaseTest {

  @Test
  public void virtualThread() {
    try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
      executor.submit(() -> println("Running inside a virtual thread: " + Thread.currentThread()));
    }
  }

  @Test
  public void structuredConcurrency() {
    try (var scope = StructuredTaskScope.open()) {
      var task1 = scope.fork(() -> println("Task1"));
      var task2 = scope.fork(() -> println("Task2"));

      scope.join();
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }

    try (var scope = StructuredTaskScope.open(Joiner.<Integer>anySuccessfulResultOrThrow())) {
      var anyTask1 = scope.fork(() -> 1 + 1);
      var anyTask2 =
          scope.fork(
              () -> {
                throw new RuntimeException("I failed! :(");
              });

      println(scope.join());

    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }
}

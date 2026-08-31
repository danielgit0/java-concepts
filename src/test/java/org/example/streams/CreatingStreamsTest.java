package org.example.streams;

import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

public class CreatingStreamsTest {

  @Test
  public void fibonacciStream() {
    IO.println("########### Example: Fibonacci stream ###########");
    Stream.iterate(new int[] {0, 1}, f -> new int[] {f[1], f[0] + f[1]})
        .limit(10)
        .map(f -> f[0])
        .forEach(IO::println);
  }

  @Test
  public void daysOfWeek() {
    IO.println("########### Ex01: Days of the week ###########");
    Stream.of("monday", "tuesday", "wednesday", "thursday", "friday", "saturday", "sunday")
        .map(String::toUpperCase)
        .forEach(IO::println);
  }

  @Test
  public void powersOfTwo() {
    IO.println("########### Ex02: Powers of 2 ###########");
    Stream.iterate(new int[] {1}, f -> new int[] {f[0] * 2})
        .limit(8)
        .map(f -> f[0])
        .forEach(IO::println);
  }

  @Test
  public void rangeClosed() {
    IO.println("########### Ex03: Range closed ###########");
    IntStream.rangeClosed(1, 20).filter(n -> n % 3 == 0).forEach(IO::println);
  }
}

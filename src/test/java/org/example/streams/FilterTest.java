package org.example.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.streams.data.StreamPractice.EMPLOYEES;
import static org.example.streams.data.StreamPractice.ORDERS;
import static org.example.streams.entities.OrderStatus.CANCELLED;

import java.util.List;
import java.util.stream.Stream;
import org.example.streams.entities.Employee;
import org.example.streams.entities.Order;
import org.junit.jupiter.api.Test;
import org.junit.platform.commons.util.StringUtils;

public class FilterTest {

  @Test
  public void fibonacciStream() {
    IO.println("########### Example: High earners in IT ###########");
    List<Employee> highEarners =
        EMPLOYEES.stream().filter(e -> e.department().equals("IT") && e.salary() > 80_000).toList();

    assertThat(highEarners).extracting(Employee::department).containsOnly("IT");

    assertThat(highEarners)
        .extracting(Employee::salary)
        .allSatisfy(salary -> assertThat(salary).isGreaterThan(80_000));
  }

  @Test
  public void filterOutCancelledOrder() {
    IO.println("########### Ex01: Filter out cancelled orders ###########");
    List<Order> ordersWithoutCancellation =
        ORDERS.stream().filter(o -> !o.status().equals(CANCELLED)).toList();

    assertThat(ordersWithoutCancellation).extracting(Order::status).doesNotContain(CANCELLED);
  }

  @Test
  public void filterBlankAndEmptyStrings() {
    IO.println("########### Ex02: Filter blank/empty strings ###########");
    List<String> strings =
        Stream.of(null, "", "a", " ", "b", "c").filter(StringUtils::isNotBlank).toList();

    assertThat(strings).containsExactly("a", "b", "c");
  }

  @Test
  public void employeesOlderThan30WithJava() {
    IO.println("########### Ex03: Filter employees older than 30 with java skills ###########");
    List<Employee> employees =
        EMPLOYEES.stream().filter(e -> e.age() > 30 && e.skills().contains("Java")).toList();

    assertThat(employees)
        .extracting(Employee::age)
        .allSatisfy(age -> assertThat(age).isGreaterThan(30));

    assertThat(employees)
        .extracting(Employee::skills)
        .allSatisfy(skills -> assertThat(skills).contains("Java"));
  }
}

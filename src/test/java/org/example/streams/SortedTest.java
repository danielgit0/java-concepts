package org.example.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.streams.data.StreamPractice.EMPLOYEES;
import static org.example.streams.data.StreamPractice.ORDERS;

import java.util.Comparator;
import java.util.List;
import org.example.streams.entities.Employee;
import org.example.streams.entities.Order;
import org.junit.jupiter.api.Test;

public class SortedTest {

  @Test
  public void sortEmployeesBySalaryAndName() {
    IO.println(
        "########### Example: List employees sorted by salary descending, then by name for ties. ###########");
    List<Employee> sortedBySalaryAndName =
        EMPLOYEES.stream()
            .sorted(
                Comparator.comparingDouble(Employee::salary)
                    .reversed()
                    .thenComparing(Employee::name))
            .toList();

    assertThat(sortedBySalaryAndName)
        .isSortedAccordingTo(
            Comparator.comparing(Employee::salary).reversed().thenComparing(Employee::name));

    sortedBySalaryAndName.forEach(e -> IO.println(e.salary() + " " + e.name()));
  }

  @Test
  public void sortOrdersByDateAsc() {
    IO.println("########### Ex01: Sort orders by orderDate ascending ###########");
    List<Order> sortedOrders =
        ORDERS.stream().sorted(Comparator.comparing(Order::orderDate)).toList();

    assertThat(sortedOrders).isSortedAccordingTo(Comparator.comparing(Order::orderDate));

    sortedOrders.forEach(o -> IO.println(o.orderDate()));
  }

  @Test
  public void sortByDepartmentAndSalaryDesc() {
    IO.println(
        "########### Ex02: Sort employees by department, then by salary descending within each department ###########");
    List<Employee> sorted =
        EMPLOYEES.stream()
            .sorted(
                Comparator.comparing(Employee::department)
                    .thenComparing(Employee::salary, Comparator.reverseOrder()))
            .toList();

    assertThat(sorted)
        .isSortedAccordingTo(
            Comparator.comparing(Employee::department)
                .thenComparing(Employee::salary, Comparator.reverseOrder()));

    sorted.forEach(e -> IO.println(e.department() + " " + e.salary()));
  }

  @Test
  public void sortByLengthAndThenAlphabeticallyForEqualLengths() {
    IO.println(
        "########### Ex03: Sort a list of words by length, then alphabetically for equal lengths. ###########");
    List<String> words =
        List.of(
            "elephant",
            "cat",
            "dog",
            "apple",
            "banana",
            "at",
            "bat",
            "ox",
            "hippopotamus",
            "ant",
            "bear",
            "fox",
            "zebra",
            "lion",
            "tiger");

    List<String> sortedWords =
        words.stream()
            .sorted(Comparator.comparing(String::length).thenComparing(Comparator.naturalOrder()))
            .toList();

    assertThat(sortedWords)
        .containsExactly(
            "at",
            "ox",
            "ant",
            "bat",
            "cat",
            "dog",
            "fox",
            "bear",
            "lion",
            "apple",
            "tiger",
            "zebra",
            "banana",
            "elephant",
            "hippopotamus");
  }
}

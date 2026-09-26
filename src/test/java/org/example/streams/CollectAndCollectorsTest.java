package org.example.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.streams.data.StreamPractice.EMPLOYEES;
import static org.example.streams.data.StreamPractice.ORDERS;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.example.streams.entities.Employee;
import org.example.streams.entities.Order;
import org.example.streams.entities.OrderStatus;
import org.junit.jupiter.api.Test;

public class CollectAndCollectorsTest {

  @Test
  public void sortEmployeesBySalaryAndName() {
    IO.println(
        "########### Example #1: Group employees by department, and within each group compute average salary. ###########");
    Map<String, Double> avgSalaryByDept =
        EMPLOYEES.stream()
            .collect(
                Collectors.groupingBy(
                    Employee::department, Collectors.averagingDouble(Employee::salary)));

    assertThat(avgSalaryByDept)
        .containsEntry("Finance", 102500.0)
        .containsEntry("HR", 58500.0)
        .containsEntry("IT", 92333.33333333333)
        .containsEntry("Marketing", 60000.0);

    IO.println("########### Example #2: Partition by order amount greater than 100. ###########");
    Map<Boolean, List<Order>> partitionedByAmount =
        ORDERS.stream().collect(Collectors.partitioningBy(o -> o.amount() > 100));

    assertThat(partitionedByAmount.get(true).size() + partitionedByAmount.get(false).size())
        .isEqualTo(8);
    assertThat(partitionedByAmount.get(false)).hasSize(3);
    assertThat(partitionedByAmount.get(true)).hasSize(5);
    assertThat(partitionedByAmount.get(true))
        .extracting(Order::amount)
        .allMatch(aDouble -> aDouble > 100);
  }

  @Test
  void shouldGroupOrdersByStatus() {
    IO.println("Group orders by status into Map<OrderStatus, List<Order>>.");
    Map<OrderStatus, List<Order>> actual =
        ORDERS.stream().collect(Collectors.groupingBy(Order::status));

    IO.println(actual);
    assertThat(actual).containsKeys(OrderStatus.values());
    assertThat(actual.get(OrderStatus.DELIVERED)).hasSize(3);
    assertThat(actual.get(OrderStatus.PENDING)).hasSize(2);
    assertThat(actual.get(OrderStatus.SHIPPED)).hasSize(2);
    assertThat(actual.get(OrderStatus.CANCELLED)).hasSize(1);
  }

  @Test
  void shouldCountEmployeesByDepartment() {
    IO.println("Group employees by department and count how many are in each (Map<String, Long>).");

    Map<String, Long> actual =
        EMPLOYEES.stream()
            .collect(Collectors.groupingBy(Employee::department, Collectors.counting()));

    IO.println(actual);

    assertThat(actual)
        .containsEntry("Finance", 2L)
        .containsEntry("HR", 2L)
        .containsEntry("IT", 3L)
        .containsEntry("Marketing", 1L);
  }

  @Test
  void shouldPartitionOrdersByValueAndPrintCounts() {
    IO.println(
        "Partition orders into \"high value\" (> $500) and \"regular\", then print counts for each partition.");

    Map<Boolean, Long> actual =
        ORDERS.stream()
            .collect(
                Collectors.partitioningBy(order -> order.amount() > 500, Collectors.counting()));

    assertThat(actual.get(false)).isEqualTo(5);
    assertThat(actual.get(true)).isEqualTo(3);
  }

  @Test
  void shouldMapEmployeeNamesToDepartments() {
    IO.println("Build a Map<String, String> of employee name → department using Collectors.toMap.");
    Map<String, String> actual =
        EMPLOYEES.stream().collect(Collectors.toMap(Employee::name, Employee::department));

    assertThat(actual)
        .containsEntry("Hannah Abbott", "HR")
        .containsEntry("Evan Wright", "Marketing")
        .containsEntry("Fiona Gallagher", "IT")
        .containsEntry("Bob Jones", "HR")
        .containsEntry("Diana Prince", "Finance")
        .containsEntry("Alice Smith", "IT")
        .containsEntry("Charlie Brown", "IT")
        .containsEntry("George Clark", "Finance");
  }

  @Test
  void shouldGroupEmployeeNamesByDepartment() {
    IO.println(
        "Group employees by department, but instead of a List<Employee>, produce a List<String> of just their names (Collectors.mapping as downstream).");

    Map<String, List<String>> actual =
        EMPLOYEES.stream()
            .collect(
                Collectors.groupingBy(
                    Employee::department, Collectors.mapping(Employee::name, Collectors.toList())));

    IO.println(actual);

    assertThat(actual).containsKeys("Finance", "HR", "IT", "Marketing");
    assertThat(actual.get("Finance")).contains("Diana Prince", "George Clark");
    assertThat(actual.get("HR")).contains("Bob Jones", "Hannah Abbott");
    assertThat(actual.get("IT")).contains("Alice Smith", "Charlie Brown", "Fiona Gallagher");
    assertThat(actual.get("Marketing")).contains("Evan Wright");
  }

  @Test
  void shouldJoinCustomerNamesIntoSingleString() {
    IO.println("Join all customer names into a single string like \"Alice, Bob, Carol\".");

    String actual = ORDERS.stream().map(Order::customer).collect(Collectors.joining(", "));

    assertThat(actual)
        .isEqualTo(
            "John Doe, Jane Watson, John Doe, Bob Vance, Alice Cooper, Jane Watson, Charlie Green, Bob Vance");
  }
}

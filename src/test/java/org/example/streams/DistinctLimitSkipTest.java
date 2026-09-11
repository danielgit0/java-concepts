package org.example.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.example.streams.data.StreamPractice.EMPLOYEES;
import static org.example.streams.data.StreamPractice.ORDERS;

import java.util.Comparator;
import java.util.List;
import org.example.streams.entities.Employee;
import org.example.streams.entities.Order;
import org.example.streams.impl.PaginationImpl;
import org.junit.jupiter.api.Test;

public class DistinctLimitSkipTest {

  @Test
  public void paginateOrders() {
    IO.println(
        "########### Example: Paginate orders — page 2, page size 3 (skip first 1, take next 3). ###########");

    IO.println("## allOrdersIdsSortedByDate ##");
    ORDERS.stream()
        .sorted(Comparator.comparing(Order::orderDate))
        .forEach(o -> IO.println(o.orderId() + " " + o.orderDate()));

    List<Order> page2 =
        ORDERS.stream().sorted(Comparator.comparing(Order::orderDate)).skip(2).limit(2).toList();

    IO.println("## page2 ##");
    page2.forEach(o -> IO.println(o.orderId() + " " + o.orderDate()));

    assertThat(page2).extracting(Order::orderId).containsExactly("ORD004", "ORD001");
  }

  @Test
  public void top3HighestPaidEmployees() {
    IO.println("########### Ex01:  Get the top 3 highest-paid employees. ###########");

    IO.println("## all ##");
    EMPLOYEES.stream()
        .sorted(Comparator.comparing(Employee::salary).reversed())
        .forEach(e -> IO.println(e.name() + " " + e.salary()));

    IO.println("## top 3 ##");
    List<Employee> top3EmployeesBySalary =
        EMPLOYEES.stream()
            .sorted(Comparator.comparing(Employee::salary).reversed())
            .limit(3)
            .toList();

    top3EmployeesBySalary.forEach(IO::println);

    assertThat(top3EmployeesBySalary)
        .extracting(Employee::name)
        .containsExactly("Charlie Brown", "George Clark", "Diana Prince");
  }

  @Test
  public void removeDuplicateCustomerNamesFromOrdersList() {
    IO.println(
        "########### Ex02:  Remove duplicate customer names from a list of orders. ###########");

    List<String> uniqueNames = ORDERS.stream().map(Order::customer).distinct().toList();

    assertThat(uniqueNames)
        .containsExactlyInAnyOrder(
            "John Doe", "Jane Watson", "Bob Vance", "Alice Cooper", "Charlie Green");
  }

  @Test
  public void implementPagination() {
    IO.println(
        "########### Ex03: Implement pagination: given page and pageSize, return the correct slice of a product list. ###########");

    List<String> items =
        List.of(
            "Apple",
            "Banana",
            "Carrot",
            "Dragonfruit",
            "Eggs",
            "Flashlight",
            "Glasses",
            "Hammer",
            "Ink",
            "Journal");

    PaginationImpl paginator = new PaginationImpl();

    List<String> actual1 = paginator.getPageSortedAsc(items, 2, 5);
    assertThat(actual1)
        .containsExactlyInAnyOrder("Flashlight", "Glasses", "Hammer", "Ink", "Journal");

    List<String> actual2 = paginator.getPageSortedAsc(items, 2, 3);
    assertThat(actual2).containsExactlyInAnyOrder("Dragonfruit", "Eggs", "Flashlight");

    assertThatThrownBy(() -> paginator.getPageSortedAsc(items, 2, 10))
        .isInstanceOf(IllegalArgumentException.class);

    assertThatThrownBy(() -> paginator.getPageSortedAsc(items, -1, 10))
        .isInstanceOf(IllegalArgumentException.class);
  }
}

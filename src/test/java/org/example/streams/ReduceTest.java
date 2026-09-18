package org.example.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.streams.data.StreamPractice.EMPLOYEES;
import static org.example.streams.data.StreamPractice.ORDERS;
import static org.example.streams.entities.OrderStatus.DELIVERED;

import java.util.Optional;
import org.assertj.core.util.Strings;
import org.example.streams.entities.Employee;
import org.example.streams.entities.Order;
import org.junit.jupiter.api.Test;

public class ReduceTest {

  @Test
  public void totalRevenueOfAllDeliveredOrders() {
    IO.println("########### Example: Total revenue from delivered orders ###########");
    double totalRevenue =
        ORDERS.stream()
            .filter(o -> o.status().equals(DELIVERED))
            .map(Order::amount)
            .reduce(0.0, Double::sum);

    assertThat(totalRevenue).isEqualTo(660.4);

    Optional<Order> mostExpensive =
        ORDERS.stream().reduce((o1, o2) -> o1.amount() >= o2.amount() ? o1 : o2);

    assertThat(mostExpensive.isPresent()).isTrue();
    assertThat(mostExpensive.get().orderId()).isEqualTo("ORD002");
    assertThat(mostExpensive.get().amount()).isEqualTo(1200.00);
  }

  @Test
  public void concatenateEmployeeNames() {
    IO.println("########### Ex01: Concatenate employee names ###########");
    Optional<String> employeeNames =
        EMPLOYEES.stream().map(Employee::name).reduce((s1, s2) -> Strings.concat(s1, ",", s2));

    assertThat(employeeNames.isPresent()).isTrue();
    assertThat(employeeNames.get())
        .isEqualTo(
            "Alice Smith,Bob Jones,Charlie Brown,Diana Prince,Evan Wright,Fiona Gallagher,George Clark,Hannah Abbott");
  }

  @Test
  public void employeeWithMaxNumberOfSkills() {
    IO.println("########### Ex02: Employee with max number of skills ###########");
    Optional<Employee> mostSkilledEmployee =
        EMPLOYEES.stream().reduce((e1, e2) -> e1.skills().size() >= e2.skills().size() ? e1 : e2);

    assertThat(mostSkilledEmployee.isPresent()).isTrue();
    assertThat(mostSkilledEmployee.get().name()).isEqualTo("Charlie Brown");
    assertThat(mostSkilledEmployee.get().skills().size()).isEqualTo(4);
  }

  @Test
  public void computeTheProductOfAListOfIntegers() {
    IO.println(
        "########### Ex03: Compute the product of a list of integers (factorial-style) ###########");

    Long ageFactorial =
        EMPLOYEES.stream()
            .mapToLong(employee -> (long) employee.age())
            .reduce(1L, Math::multiplyExact);

    assertThat(ageFactorial).isEqualTo(1_202_592_300_000L);
  }
}

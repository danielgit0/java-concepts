package org.example.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.streams.data.StreamPractice.EMPLOYEES;
import static org.example.streams.data.StreamPractice.ORDERS;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import org.example.streams.entities.Employee;
import org.junit.jupiter.api.Test;

public class MapAndMapToTest {

  @Test
  public void fibonacciStream() {
    IO.println("########### Example: map and mapToX ###########");
    List<String> names = EMPLOYEES.stream().map(Employee::name).toList();

    double totalBonus = EMPLOYEES.stream().mapToDouble(e -> e.salary() * 0.10).sum();
  }

  @Test
  public void ordersToFormattedStrings() {
    IO.println(
        "########### Ex01:  List of Orders into a list of formatted strings like \"ORD-101: $250.00\" ###########");
    List<String> ordersStringList =
        ORDERS.stream().map(o -> o.orderId() + ": " + o.amount()).toList();

    IO.println(ordersStringList);
  }

  @Test
  public void convertUsdToEur() {
    List<BigDecimal> usdPrices =
        List.of(
            BigDecimal.valueOf(19.5),
            BigDecimal.valueOf(20.46),
            BigDecimal.valueOf(500.22),
            BigDecimal.valueOf(1000));

    List<BigDecimal> eurPrices =
        usdPrices.stream()
            .map(p -> p.multiply(BigDecimal.valueOf(0.92)).setScale(2, RoundingMode.HALF_EVEN))
            .toList();

    assertThat(eurPrices)
        .usingComparatorForType(BigDecimal::compareTo, BigDecimal.class)
        .containsExactly(
            new BigDecimal("17.94"),
            new BigDecimal("18.82"),
            new BigDecimal("460.20"),
            new BigDecimal("920.00"));
  }

  @Test
  public void totalSkills() {
    IO.println("########### Ex03: Get the total number of skills across all employees ###########");
    int totalBonus = EMPLOYEES.stream().mapToInt(e -> e.skills().size()).sum();

    assertThat(totalBonus).isEqualTo(22);
  }
}

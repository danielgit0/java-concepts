package org.example.streams;

import static org.assertj.core.api.Assertions.assertThat;
import static org.example.streams.data.StreamPractice.EMPLOYEES;
import static org.example.streams.data.StreamPractice.ORDERS;

import java.util.Collection;
import java.util.List;
import org.junit.jupiter.api.Test;

public class FlatMapTest {

  @Test
  public void distinctListOfSkills() {
    IO.println(
        "########### Example: Get a distinct, sorted list of every skill across all employees ###########");
    List<String> allSkills =
        EMPLOYEES.stream().flatMap(e -> e.skills().stream()).distinct().sorted().toList();

    assertThat(allSkills)
        .containsExactly(
            "AWS",
            "Analysis",
            "Architecture",
            "Communication",
            "Conflict Resolution",
            "Copywriting",
            "Design",
            "Django",
            "Docker",
            "Excel",
            "Forecasting",
            "Java",
            "Kubernetes",
            "Onboarding",
            "Python",
            "Recruiting",
            "Risk Management",
            "SEO",
            "SQL",
            "Spring");
  }

  @Test
  public void listOfItemIds() {
    IO.println(
        "########### Ex01:  Given List<List<Integer>> representing shopping carts per customer, flatten into one list of all item IDs. ###########");
    List<List<Integer>> shoppingCarts = List.of(List.of(1, 3, 4), List.of(2, 5), List.of(6, 7, 8));

    List<Integer> allItemIds = shoppingCarts.stream().flatMap(Collection::stream).toList();

    assertThat(allItemIds).containsExactlyInAnyOrder(1, 2, 3, 4, 5, 6, 7, 8);
  }

  @Test
  public void sentencesToStreamOfWords() {
    IO.println(
        "########### Ex02:  Given a list of sentences (List<String>), use flatMap to produce a stream of individual words. ###########");
    List<String> sentences =
        List.of(
            "Hola, ¿cómo estás?",
            "El lenguaje Java es muy potente.",
            "Me gusta aprender a programar.");

    List<String> words =
        sentences.stream().map(s -> List.of(s.split(" "))).flatMap(Collection::stream).toList();

    assertThat(words)
        .containsExactlyInAnyOrder(
            "Hola,",
            "¿cómo",
            "estás?",
            "El",
            "lenguaje",
            "Java",
            "es",
            "muy",
            "potente.",
            "Me",
            "gusta",
            "aprender",
            "a",
            "programar.");
  }

  @Test
  public void totalSkills() {
    IO.println(
        "########### Ex03: From orders, where each order has a List<String> items, find all unique item names ordered by any customer. ###########");

    List<String> itemsNames =
        ORDERS.stream().flatMap(o -> o.items().stream()).distinct().sorted().toList();

    assertThat(itemsNames)
        .containsExactly(
            "Desk Chair",
            "HDMI Cable",
            "Headphones",
            "Keyboard",
            "Laptop",
            "Monitor",
            "Mouse",
            "Mouse Pad",
            "USB Drive");
  }
}

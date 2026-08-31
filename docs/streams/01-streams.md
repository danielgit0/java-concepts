# Java Streams — Concepts, Real-Life Examples & Exercises

A hands-on compilation covering the Java Streams API: what each operation does, a real-world example, and practice problems (including `parallelStream()`). Solutions are provided below each problem set so you can try first, then check.

We'll use a common domain model throughout:

```java
public class Employee {
    private String name;
    private String department;
    private double salary;
    private int age;
    private List<String> skills;

    // constructor, getters, setters, toString omitted for brevity
}

public class Order {
    private String orderId;
    private String customer;
    private double amount;
    private String status; // "PENDING", "SHIPPED", "DELIVERED", "CANCELLED"
    private LocalDate orderDate;
}
```

---

## Table of Contents

1. [Creating Streams](#1-creating-streams)
2. [filter()](#2-filter)
3. [map() and mapToXxx()](#3-map-and-maptoxxx)
4. [flatMap()](#4-flatmap)
5. [sorted()](#5-sorted)
6. [distinct(), limit(), skip()](#6-distinct-limit-skip)
7. [reduce()](#7-reduce)
8. [collect() and Collectors](#8-collect-and-collectors)
9. [Matching: anyMatch / allMatch / noneMatch](#9-matching-anymatch--allmatch--nonematch)
10. [findFirst() / findAny()](#10-findfirst--findany)
11. [Optional](#11-optional)
12. [IntStream / Numeric Streams & Statistics](#12-intstream--numeric-streams--statistics)
13. [Parallel Streams](#13-parallel-streams)
14. [Mixed Capstone Problems](#14-mixed-capstone-problems)

---

## 1. Creating Streams

Streams can be created from collections, arrays, static factory methods, or generators.

```java
Stream<String> s1 = List.of("a", "b", "c").stream();
Stream<Integer> s2 = Stream.of(1, 2, 3);
IntStream s3 = IntStream.range(1, 10);        // 1..9
IntStream s4 = IntStream.rangeClosed(1, 10);  // 1..10
Stream<Double> s5 = Stream.generate(Math::random).limit(5);
Stream<Integer> s6 = Stream.iterate(1, n -> n * 2).limit(5); // 1,2,4,8,16
```

**Real-life example:** Generating the first 10 Fibonacci numbers to seed a chart.

```java
Stream.iterate(new int[]{0, 1}, f -> new int[]{f[1], f[0] + f[1]})
      .limit(10)
      .map(f -> f[0])
      .forEach(System.out::println);
```

### Exercises
1. Create a stream of the days of the week from an array and print each in uppercase.
2. Use `Stream.iterate` to generate the first 8 powers of 2.
3. Use `IntStream.rangeClosed` to print numbers from 1 to 20 that are divisible by 3.

---

## 2. filter()

Keeps only elements matching a predicate. Doesn't transform elements, just selects them.

**Real-life example:** From a list of employees, find everyone in the "IT" department earning more than $80,000.

```java
List<Employee> highEarners = employees.stream()
        .filter(e -> e.getDepartment().equals("IT"))
        .filter(e -> e.getSalary() > 80_000)
        .collect(Collectors.toList());
```

### Exercises
1. From a list of `Order`s, filter out all `CANCELLED` orders.
2. Given a list of strings, filter out all blank/empty strings.
3. From `employees`, filter those older than 30 who know "Java" (hint: `skills.contains("Java")`).

---

## 3. map() and mapToXxx()

Transforms each element into another form (1-to-1 mapping). `mapToInt`/`mapToDouble`/`mapToLong` convert to primitive streams (avoiding boxing, enabling `sum()`, `average()`, etc.).

**Real-life example:** Extract just the names of employees, or compute a 10% bonus for each salary.

```java
List<String> names = employees.stream()
        .map(Employee::getName)
        .collect(Collectors.toList());

double totalBonus = employees.stream()
        .mapToDouble(e -> e.getSalary() * 0.10)
        .sum();
```

### Exercises
1. Convert a list of `Order`s into a list of formatted strings like `"ORD-101: $250.00"`.
2. Given a list of prices in USD, use `map` to convert them all to EUR (rate 0.92).
3. Use `mapToInt` to get the total number of skills across all employees.

---

## 4. flatMap()

Flattens nested structures (a stream of collections becomes a single stream of elements). Use when `map` would give you `Stream<List<X>>` but you want `Stream<X>`.

**Real-life example:** Get a distinct, sorted list of every skill across all employees (each employee has a `List<String> skills`).

```java
List<String> allSkills = employees.stream()
        .flatMap(e -> e.getSkills().stream())
        .distinct()
        .sorted()
        .collect(Collectors.toList());
```

### Exercises
1. Given `List<List<Integer>>` representing shopping carts per customer, flatten into one list of all item IDs.
2. Given a list of sentences (`List<String>`), use `flatMap` to produce a stream of individual words.
3. From `orders`, where each order has a `List<String> items`, find all unique item names ordered by any customer.

---

## 5. sorted()

Sorts a stream, either naturally (`Comparable`) or via a `Comparator`. Streams are lazy — sorting only happens when a terminal operation runs.

**Real-life example:** List employees sorted by salary descending, then by name for ties.

```java
List<Employee> sortedBySalary = employees.stream()
        .sorted(Comparator.comparingDouble(Employee::getSalary).reversed()
                .thenComparing(Employee::getName))
        .collect(Collectors.toList());
```

### Exercises
1. Sort `orders` by `orderDate` ascending.
2. Sort employees by department, then by salary descending within each department.
3. Sort a list of words by length, then alphabetically for equal lengths.

---

## 6. distinct(), limit(), skip()

- `distinct()` — removes duplicates (uses `equals()`).
- `limit(n)` — truncates to the first n elements.
- `skip(n)` — skips the first n elements (useful for pagination).

**Real-life example:** Paginate orders — page 2, page size 5 (skip first 5, take next 5).

```java
List<Order> page2 = orders.stream()
        .sorted(Comparator.comparing(Order::getOrderDate))
        .skip(5)
        .limit(5)
        .collect(Collectors.toList());
```

### Exercises
1. Get the top 3 highest-paid employees.
2. Remove duplicate customer names from a list of orders.
3. Implement pagination: given `page` and `pageSize`, return the correct slice of a product list.

---

## 7. reduce()

Combines stream elements into a single result using an accumulator function. General-purpose folding operation — think of it as the Swiss army knife behind `sum`, `max`, string concatenation, etc.

**Real-life example:** Calculate the total revenue from all delivered orders, and find the most expensive order.

```java
double totalRevenue = orders.stream()
        .filter(o -> o.getStatus().equals("DELIVERED"))
        .map(Order::getAmount)
        .reduce(0.0, Double::sum);

Optional<Order> mostExpensive = orders.stream()
        .reduce((o1, o2) -> o1.getAmount() >= o2.getAmount() ? o1 : o2);
```

### Exercises
1. Use `reduce` to concatenate all employee names into a single comma-separated string.
2. Use `reduce` to find the employee with the maximum number of skills (return the `Employee`, not a count).
3. Use `reduce` with an identity value to compute the product of a list of integers (factorial-style).

---

## 8. collect() and Collectors

The most versatile terminal operation. Common collectors:

| Collector | Purpose |
|---|---|
| `toList()` / `toSet()` / `toMap()` | Gather into a collection |
| `joining(", ")` | Concatenate strings |
| `groupingBy(fn)` | Group into `Map<K, List<V>>` |
| `groupingBy(fn, downstream)` | Group + aggregate (counting, summing, mapping...) |
| `partitioningBy(predicate)` | Split into `Map<Boolean, List<V>>` |
| `counting()`, `summingDouble()`, `averagingDouble()` | Aggregations, usually used as downstream collectors |

**Real-life example:** Group employees by department, and within each group compute average salary.

```java
Map<String, Double> avgSalaryByDept = employees.stream()
        .collect(Collectors.groupingBy(
                Employee::getDepartment,
                Collectors.averagingDouble(Employee::getSalary)));

Map<Boolean, List<Order>> partitioned = orders.stream()
        .collect(Collectors.partitioningBy(o -> o.getAmount() > 100));
```

### Exercises
1. Group `orders` by `status` into `Map<String, List<Order>>`.
2. Group employees by department and count how many are in each (`Map<String, Long>`).
3. Partition orders into "high value" (> $500) and "regular", then print counts for each partition.
4. Build a `Map<String, String>` of employee name → department using `Collectors.toMap`.
5. Group employees by department, but instead of a `List<Employee>`, produce a `List<String>` of just their names (`Collectors.mapping` as downstream).
6. Join all customer names into a single string like `"Alice, Bob, Carol"`.

---

## 9. Matching: anyMatch / allMatch / noneMatch

Short-circuiting boolean checks over a stream.

**Real-life example:** Check if any order is overdue, or if all employees meet a minimum wage requirement.

```java
boolean hasOverdue = orders.stream()
        .anyMatch(o -> o.getStatus().equals("PENDING") &&
                       o.getOrderDate().isBefore(LocalDate.now().minusDays(7)));

boolean allAboveMinWage = employees.stream()
        .allMatch(e -> e.getSalary() >= 30_000);
```

### Exercises
1. Check whether any employee has "Kubernetes" as a skill.
2. Check whether none of the orders are `CANCELLED`.
3. Check whether all employees in the "HR" department are over 25 years old.

---

## 10. findFirst() / findAny()

Returns an `Optional` with an element matching criteria. `findFirst()` respects encounter order; `findAny()` may be faster on parallel streams since it doesn't need to preserve order.

**Real-life example:** Find the first pending order (to process next in a queue).

```java
Optional<Order> nextToProcess = orders.stream()
        .filter(o -> o.getStatus().equals("PENDING"))
        .sorted(Comparator.comparing(Order::getOrderDate))
        .findFirst();
```

### Exercises
1. Find any employee earning more than $150,000, or report none exists.
2. Find the first order (by date) placed by a specific customer.
3. Using `findAny()` on a `parallelStream()`, find any employee with more than 5 skills.

---

## 11. Optional

Represents a value that may or may not be present — avoids null checks and `NullPointerException`.

**Real-life example:** Safely look up an employee's manager, providing a fallback if none exists.

```java
Optional<Employee> manager = employees.stream()
        .filter(e -> e.getName().equals("Nonexistent"))
        .findFirst();

String managerName = manager.map(Employee::getName).orElse("No manager assigned");

manager.ifPresentOrElse(
        m -> System.out.println("Manager: " + m.getName()),
        () -> System.out.println("No manager found"));
```

### Exercises
1. Find the highest-paid employee in a (possibly empty) list; return `"N/A"` if the list is empty, using `Optional`.
2. Use `Optional.ofNullable` to safely process a value that might be `null` from an external API call.
3. Chain `.filter()` and `.map()` on an `Optional<Employee>` to get the department name only if salary > 100,000, else `"Unknown"`.

---

## 12. IntStream / Numeric Streams & Statistics

Primitive streams (`IntStream`, `LongStream`, `DoubleStream`) avoid autoboxing overhead and expose numeric-only operations like `sum()`, `average()`, `max()`, and `summaryStatistics()`.

**Real-life example:** Get a full statistical summary of order amounts for a dashboard (min, max, average, count, sum) in one pass.

```java
DoubleSummaryStatistics stats = orders.stream()
        .mapToDouble(Order::getAmount)
        .summaryStatistics();

System.out.printf("Orders: %d, Total: %.2f, Avg: %.2f, Min: %.2f, Max: %.2f%n",
        stats.getCount(), stats.getSum(), stats.getAverage(), stats.getMin(), stats.getMax());
```

### Exercises
1. Compute the average age of all employees using `IntStream`.
2. Get `summaryStatistics()` for employee salaries and print min, max, and average.
3. Use `IntStream.rangeClosed(1, 100)` to sum all numbers divisible by 7.

---

## 13. Parallel Streams

`parallelStream()` (or `.parallel()` on an existing stream) splits work across multiple threads using the common `ForkJoinPool`. It can speed up CPU-intensive work on large datasets, but comes with real risks:

- **Overhead** — for small collections, thread coordination costs more than it saves.
- **Ordering** — operations like `forEach` don't guarantee order on parallel streams; use `forEachOrdered` if order matters.
- **Statefulness / thread safety** — using mutable shared state (e.g., a plain `ArrayList` or a counter variable) inside a parallel stream causes race conditions. Use thread-safe collectors instead.
- **Blocking / I/O-bound work** — parallel streams are meant for CPU-bound work; blocking calls (DB, network) can starve the shared `ForkJoinPool` used elsewhere in the app.
- **Splitting cost** — some sources (like `LinkedList`) split poorly and gain little from parallelism.

**Real-life example — good use case:** Recomputing a discount price for 2 million products (pure CPU-bound, independent per element).

```java
List<Product> repriced = products.parallelStream()
        .map(p -> p.withPrice(p.getPrice() * 0.9))
        .collect(Collectors.toList());
```

**Real-life example — the classic bug:** Accumulating into a shared, non-thread-safe list.

```java
// BROKEN: ArrayList is not thread-safe — causes data loss/corruption/exceptions
List<String> results = new ArrayList<>();
employees.parallelStream()
        .map(Employee::getName)
        .forEach(results::add);   // race condition!

// FIXED: let collect() handle thread-safe accumulation
List<String> results = employees.parallelStream()
        .map(Employee::getName)
        .collect(Collectors.toList());
```

### Exercises

1. **Sequential vs. parallel timing.** Given a list of 5 million integers, compute the sum of squares using `stream()` and again using `parallelStream()`. Measure and compare execution time with `System.nanoTime()`. At what size does parallel actually win?

2. **Spot the bug.** The following code sometimes produces fewer than expected results. Explain why and fix it:
   ```java
   List<Integer> evens = new ArrayList<>();
   IntStream.rangeClosed(1, 100_000)
            .parallel()
            .filter(n -> n % 2 == 0)
            .forEach(evens::add);
   System.out.println(evens.size()); // often != 50000, or throws
   ```

3. **Ordering pitfall.** Given `List.of(1,2,3,4,5,6,7,8,9,10).parallelStream()`, print the elements with `.forEach()` and then with `.forEachOrdered()`. Explain the difference in output.

4. **Thread-safe aggregation.** Using `employees.parallelStream()`, group employees by department and compute the total salary per department **without** using a plain `HashMap` you mutate manually — use `Collectors.groupingBy` + `Collectors.summingDouble` instead, and explain why this is safe on a parallel stream while manual mutation isn't.

5. **CPU-bound benchmark.** Write a (deliberately slow) prime-checking function `isPrime(long n)`. Use it to count how many primes exist below 1,000,000 with a sequential stream, then a parallel stream. Compare timing and explain why this workload benefits from parallelism (unlike I/O-bound work).

6. **When NOT to parallelize.** Given a list of just 20 employees, benchmark `stream()` vs `parallelStream()` for a simple `filter + map` operation. Explain why the parallel version is likely *slower*, referencing thread pool startup/coordination overhead.

7. **Custom ForkJoinPool.** Show how to run a parallel stream inside a custom `ForkJoinPool` (instead of the shared common pool) so it doesn't interfere with other parallel work in the application:
   ```java
   ForkJoinPool customPool = new ForkJoinPool(4);
   long total = customPool.submit(() ->
           bigList.parallelStream().mapToLong(Long::longValue).sum()
   ).get();
   ```
   Explain a scenario in a web server where this matters.

---

## 14. Mixed Capstone Problems

These combine multiple concepts, closer to real interview/work scenarios.

1. **Payroll report.** Given `employees`, produce a `Map<String, Double>` of department → total payroll, sorted by total payroll descending (hint: you'll need `LinkedHashMap` and a sorted stream of the map's entries).

2. **Top earner per department.** Produce a `Map<String, Employee>` mapping each department to its highest-paid employee (`Collectors.groupingBy` + `Collectors.maxBy`).

3. **Order dashboard.** Given `orders`, in a single pipeline (or a small number of pipelines) compute:
    - total revenue from `DELIVERED` orders only
    - the count of orders per status
    - the top 3 customers by total spend

4. **Skill demand report.** Given `employees`, find the 5 most common skills across the company and how many employees have each (`flatMap` + `groupingBy` + `counting()`, then `sorted` + `limit`).

5. **Parallel batch pricing.** You have 1 million `Product` records that each need a moderately expensive recalculation (simulate with a small `Thread.sleep`-free CPU loop, not real sleep). Using `parallelStream()`, recompute prices and collect into a `Map<String, Double>` of product ID → new price, ensuring the collector you choose is safe for parallel use.

---

## Quick Reference Cheat Sheet

```java
list.stream()
    .filter(predicate)          // keep matching elements
    .map(function)               // transform elements
    .flatMap(function)           // flatten nested streams
    .distinct()                  // remove duplicates
    .sorted()                    // natural or comparator order
    .limit(n) / .skip(n)         // truncate / paginate
    .peek(consumer)               // debug/side-effect (use sparingly)
    .reduce(identity, accumulator)
    .collect(Collectors.toList() / toSet() / toMap() / groupingBy() / partitioningBy() / joining())
    .anyMatch() / .allMatch() / .noneMatch()
    .findFirst() / .findAny()
    .count() / .min() / .max()
    .forEach() / .forEachOrdered()   // terminal, use ordered version on parallel streams when order matters
```

**Parallel streams rule of thumb:** use them for large, CPU-bound, independent-per-element workloads with thread-safe collectors — avoid them for small collections, I/O-bound work, or anything mutating shared state manually.

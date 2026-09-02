package org.example.streams.data;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.example.streams.entities.Employee;
import org.example.streams.entities.Order;
import org.example.streams.entities.OrderStatus;

public class StreamPractice {
  public static List<Employee> EMPLOYEES =
      Arrays.asList(
          new Employee("Alice Smith", "IT", 85000, 28, Arrays.asList("Java", "Spring", "Docker")),
          new Employee("Bob Jones", "HR", 55000, 35, Arrays.asList("Recruiting", "Communication")),
          new Employee(
              "Charlie Brown",
              "IT",
              120000,
              42,
              Arrays.asList("Java", "AWS", "Kubernetes", "Architecture")),
          new Employee(
              "Diana Prince", "Finance", 95000, 31, Arrays.asList("Excel", "SQL", "Analysis")),
          new Employee(
              "Evan Wright", "Marketing", 60000, 25, Arrays.asList("SEO", "Copywriting", "Design")),
          new Employee(
              "Fiona Gallagher", "IT", 72000, 26, Arrays.asList("Python", "Django", "SQL")),
          new Employee(
              "George Clark",
              "Finance",
              110000,
              50,
              Arrays.asList("Risk Management", "Forecasting")),
          new Employee(
              "Hannah Abbott",
              "HR",
              62000,
              29,
              Arrays.asList("Onboarding", "Conflict Resolution")));

  public static List<Order> ORDERS =
      Arrays.asList(
          new Order(
              "ORD001",
              "John Doe",
              250.50,
              OrderStatus.DELIVERED,
              LocalDate.of(2026, 8, 15),
              List.of("Monitor", "Mouse")),
          new Order(
              "ORD002",
              "Jane Watson",
              1200.00,
              OrderStatus.SHIPPED,
              LocalDate.of(2026, 8, 28),
              List.of("Laptop", "Mouse Pad")),
          new Order(
              "ORD003",
              "John Doe",
              45.99,
              OrderStatus.PENDING,
              LocalDate.of(2026, 8, 30),
              List.of("Keyboard", "Mouse")),
          new Order(
              "ORD004",
              "Bob Vance",
              850.00,
              OrderStatus.CANCELLED,
              LocalDate.of(2026, 8, 10),
              List.of("Desk Chair", "Monitor")),
          new Order(
              "ORD005",
              "Alice Cooper",
              99.90,
              OrderStatus.DELIVERED,
              LocalDate.of(2026, 7, 22),
              List.of("Headphones")),
          new Order(
              "ORD006",
              "Jane Watson",
              310.00,
              OrderStatus.DELIVERED,
              LocalDate.of(2026, 8, 5),
              List.of("Monitor", "HDMI Cable")),
          new Order(
              "ORD007",
              "Charlie Green",
              15.25,
              OrderStatus.PENDING,
              LocalDate.of(2026, 8, 31),
              List.of("USB Drive")),
          new Order(
              "ORD008",
              "Bob Vance",
              620.00,
              OrderStatus.SHIPPED,
              LocalDate.of(2026, 8, 29),
              List.of("Laptop")));
}

package org.example.streams.entities;

import java.time.LocalDate;
import java.util.List;

public record Order(
    String orderId,
    String customer,
    double amount,
    OrderStatus status,
    LocalDate orderDate,
    List<String> items) {}

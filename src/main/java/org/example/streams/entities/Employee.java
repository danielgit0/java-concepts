package org.example.streams.entities;

import java.util.List;

public record Employee(
    String name, String department, double salary, int age, List<String> skills) {}

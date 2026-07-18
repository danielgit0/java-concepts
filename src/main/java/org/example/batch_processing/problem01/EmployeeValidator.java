package org.example.batch_processing.problem01;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class EmployeeValidator {
  private static final Set<String> VALID_DEPARTMENTS =
      Set.of("Engineering", "Marketing", "Sales", "HR", "Finance");

  public static List<String> validate(String name, String department, String salaryStr) {
    List<String> errors = new ArrayList<>();

    if (name.isBlank()) {
      errors.add("empty name");
    }
    if (!VALID_DEPARTMENTS.contains(department)) {
      errors.add("invalid department");
    }
    try {
      double salary = Double.parseDouble(salaryStr);

      if (salary < 0) {
        errors.add("negative salary");
      }
    } catch (NumberFormatException e) {
      errors.add("not a number");
    }

    return errors;
  }
}

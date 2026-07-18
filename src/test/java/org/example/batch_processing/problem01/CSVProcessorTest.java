package org.example.batch_processing.problem01;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CSVProcessorTest {

  @Test
  public void validateRow() {
    assertThat(EmployeeValidator.validate("", "", ""))
        .containsExactlyInAnyOrder("not a number", "empty name", "invalid department");
    assertThat(EmployeeValidator.validate("Fulanito", "Engineering", "-1"))
        .containsExactlyInAnyOrder("negative salary");
    assertThat(EmployeeValidator.validate("Fulanito", "Engineering", "1")).hasSize(0);
  }

  @Test
  public void process(@TempDir Path tempDir) throws IOException {
    String inputPath =
        getClass().getClassLoader().getResource("batch_processing/problem01/input.csv").getPath();
    String validOutputPath = tempDir.resolve("valid.csv").toAbsolutePath().toString();
    String invalidOutputPath = tempDir.resolve("invalid.csv").toAbsolutePath().toString();

    CSVProcessor csvProcessor = new CSVProcessor(100);
    csvProcessor.processCSV(inputPath, validOutputPath, invalidOutputPath);

    Path validFilePath = tempDir.resolve("valid.csv");
    Path invalidFilePath = tempDir.resolve("invalid.csv");

    assertTrue(Files.exists(validFilePath));
    Files.lines(validFilePath).limit(10).forEach(System.out::println);

    assertTrue(Files.exists(invalidFilePath));
    Files.lines(invalidFilePath).limit(10).forEach(System.out::println);

    long validRowCount = Files.lines(validFilePath).count();
    assertThat(validRowCount).isEqualTo(262);

    long invalidRowCount = Files.lines(invalidFilePath).count();
    assertThat(invalidRowCount).isEqualTo(60);
  }
}

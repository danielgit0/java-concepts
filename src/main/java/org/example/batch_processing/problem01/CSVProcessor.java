package org.example.batch_processing.problem01;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class CSVProcessor {

  private final int batchSize;

  public CSVProcessor(int batchSize) {
    this.batchSize = batchSize;
  }

  public void processCSV(String inputFile, String validOut, String invalidOut) throws IOException {

    try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        PrintWriter validWriter = new PrintWriter(new FileWriter(validOut));
        PrintWriter invalidWriter = new PrintWriter(new FileWriter(invalidOut))) {

      validWriter.println("id,name,department,salary");
      invalidWriter.println("id,name,department,salary,reason");

      String header = reader.readLine();
      List<String> batch = new ArrayList<>();
      String line;
      int batchNumber = 0;

      while ((line = reader.readLine()) != null) {
        batch.add(line);
        if (batch.size() == batchSize) {
          processBatch(batch, ++batchNumber, validWriter, invalidWriter);
          batch.clear();
        }
      }

      if (!batch.isEmpty()) {
        processBatch(batch, ++batchNumber, validWriter, invalidWriter);
      }
    }

    System.out.println("Batch processing complete.");
  }

  private static void processBatch(
      List<String> batch, int batchNumber, PrintWriter validWriter, PrintWriter invalidWriter) {
    System.out.printf("Processing batch #%d (%d records)%n", batchNumber, batch.size());

    for (String line : batch) {
      String[] parts = line.split(",", -1);
      if (parts.length < 4) {
        invalidWriter.println(line + ",malformed record");
        continue;
      }

      String id = parts[0].trim();
      String name = parts[1].trim();
      String department = parts[2].trim();
      String salaryStr = parts[3].trim();

      List<String> errors = EmployeeValidator.validate(name, department, salaryStr);
      if (errors.isEmpty()) {
        validWriter.println(line);
      } else {
        String csvErrors =
            "\""
                + errors.stream().map(e -> e.replace("\"", "\"\"")).collect(Collectors.joining(","))
                + "\"";
        invalidWriter.printf("%s,%s%n", line, csvErrors);
      }
    }
  }
}

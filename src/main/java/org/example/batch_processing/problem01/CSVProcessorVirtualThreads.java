package org.example.batch_processing.problem01;

import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class CSVProcessorVirtualThreads {

  private final int batchSize;

  public CSVProcessorVirtualThreads(int batchSize) {
    this.batchSize = batchSize;
  }

  public void processCSV(String inputFile, String validOut, String invalidOut)
      throws IOException, InterruptedException {

    Semaphore semaphore = new Semaphore(3);

    List<Future<BatchResult>> futures = new ArrayList<>();

    try (BufferedReader reader = new BufferedReader(new FileReader(inputFile));
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor()) {

      reader.readLine(); // skip header

      List<String> batch = new ArrayList<>();
      String line;
      int batchNumber = 0;

      while ((line = reader.readLine()) != null) {
        batch.add(line);

        if (batch.size() == batchSize) {
          List<String> batchCopy = List.copyOf(batch);
          int currentBatch = ++batchNumber;

          futures.add(
              executor.submit(
                  () -> {
                    semaphore.acquire();
                    try {
                      return processBatch(batchCopy, currentBatch);
                    } finally {
                      semaphore.release();
                    }
                  }));

          batch.clear();
        }
      }

      if (!batch.isEmpty()) {
        List<String> batchCopy = List.copyOf(batch);
        int currentBatch = ++batchNumber;

        futures.add(
            executor.submit(
                () -> {
                  semaphore.acquire();
                  try {
                    return processBatch(batchCopy, currentBatch);
                  } finally {
                    semaphore.release();
                  }
                }));
      }

      try (PrintWriter validWriter = new PrintWriter(new FileWriter(validOut));
          PrintWriter invalidWriter = new PrintWriter(new FileWriter(invalidOut))) {

        validWriter.println("id,name,department,salary");
        invalidWriter.println("id,name,department,salary,reason");

        for (Future<BatchResult> future : futures) {
          BatchResult result = future.get();

          result.validRecords.forEach(validWriter::println);
          result.invalidRecords.forEach(invalidWriter::println);
        }
      } catch (ExecutionException e) {
        throw new RuntimeException(e);
      }
    }

    System.out.println("Batch processing complete.");
  }

  private static BatchResult processBatch(List<String> batch, int batchNumber) {
    System.out.printf("Processing batch #%d (%d records)%n", batchNumber, batch.size());

    List<String> validRecords = new ArrayList<>();
    List<String> invalidRecords = new ArrayList<>();

    for (String line : batch) {
      String[] parts = line.split(",", -1);

      if (parts.length < 4) {
        invalidRecords.add(line + ",malformed record");
        continue;
      }

      String name = parts[1].trim();
      String department = parts[2].trim();
      String salaryStr = parts[3].trim();

      List<String> errors = EmployeeValidator.validate(name, department, salaryStr);

      if (errors.isEmpty()) {
        validRecords.add(line);
      } else {
        String csvErrors =
            "\""
                + errors.stream().map(e -> e.replace("\"", "\"\"")).collect(Collectors.joining(","))
                + "\"";

        invalidRecords.add(line + "," + csvErrors);
      }
    }

    return new BatchResult(validRecords, invalidRecords);
  }

  private record BatchResult(List<String> validRecords, List<String> invalidRecords) {}
}

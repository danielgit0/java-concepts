# Java Batch Processing — Coding Problems & Solutions

---

## Problem 1: Batch CSV Processor with Error Handling

### Description

You are given a large CSV file containing employee records with the following columns:
`id, name, department, salary`

Write a Java program that reads the file in **batches of 100 records**, validates each record (non-empty name, valid department from a whitelist, salary > 0), and writes:
- Valid records to `valid_employees.csv`
- Invalid records (with a reason) to `invalid_employees.csv`

The program must log how many records were processed per batch.

### Constraints

- Do **not** load the entire file into memory.
- Use buffered I/O for performance.
- Handle `IOException` gracefully without crashing the entire batch.

### Key Concepts Demonstrated

- **Streaming I/O** — `BufferedReader` reads line-by-line, avoiding full in-memory load.
- **Batch accumulation** — records are collected into a `List` and flushed every 100 rows.
- **Dual-output routing** — clean separation between valid and invalid records.
- **Graceful partial failures** — a bad record is rejected without aborting the batch.

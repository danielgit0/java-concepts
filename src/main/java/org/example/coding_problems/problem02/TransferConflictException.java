package org.example.coding_problems.problem02;

public class TransferConflictException extends RuntimeException {

  public TransferConflictException(String message) {
    super("Could not acquire locks, retry");
  }
}

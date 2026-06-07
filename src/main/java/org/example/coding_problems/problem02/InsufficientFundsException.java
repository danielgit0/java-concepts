package org.example.coding_problems.problem02;

public class InsufficientFundsException extends RuntimeException {

  public InsufficientFundsException(String accountId) {
    super("Account %s has insufficient funds.".formatted(accountId));
  }
}

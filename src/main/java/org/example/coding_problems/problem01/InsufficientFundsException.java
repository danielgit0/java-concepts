package org.example.coding_problems.problem01;

public class InsufficientFundsException extends RuntimeException {

  public InsufficientFundsException(String accountId) {
    super("Account %s has insufficient funds.".formatted(accountId));
  }
}

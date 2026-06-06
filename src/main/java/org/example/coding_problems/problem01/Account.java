package org.example.coding_problems.problem01;

public class Account {
  private final String id;
  private long balance;

  public Account(String id, long balance) {
    this.id = id;
    this.balance = balance;
  }

  public long getBalance() {
    return balance;
  }

  public void credit(final long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Negative amounts are not allowed");
    }
    balance += amount;
  }

  public void debit(final long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Negative amounts are not allowed");
    }
    if (balance < amount) {
      throw new InsufficientFundsException(id);
    }
    balance -= amount;
  }
}

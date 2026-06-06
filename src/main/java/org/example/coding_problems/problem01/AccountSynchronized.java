package org.example.coding_problems.problem01;

public class AccountSynchronized {
  private final String id;
  private long balance;

  public AccountSynchronized(String id, long balance) {
    this.id = id;
    this.balance = balance;
  }

  public synchronized long getBalance() {
    return balance;
  }

  public synchronized void credit(final long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Negative amounts are not allowed");
    }
    balance += amount;
  }

  public synchronized void debit(final long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Negative amounts are not allowed");
    }
    if (balance < amount) {
      throw new InsufficientFundsException(id);
    }
    balance -= amount;
  }
}

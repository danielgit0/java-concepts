package org.example.coding_problems.problem02;

import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;

public class AccountReentrantLock {
  private final String id;
  private long balance;
  private final ReentrantLock lock = new ReentrantLock();

  public AccountReentrantLock(String id, long balance) {
    this.id = id;
    this.balance = balance;
  }

  public String getId() {
    return id;
  }

  public long getBalance() {
    return balance;
  }

  public ReentrantLock getLock() {
    return lock;
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

  @Override
  public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AccountReentrantLock account = (AccountReentrantLock) o;
    return Objects.equals(id, account.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }
}

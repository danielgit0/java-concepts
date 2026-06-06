package org.example.coding_problems.problem01;

import java.util.concurrent.atomic.AtomicLong;

public class AccountAtomic {
  private final String id;
  private AtomicLong balance;

  public AccountAtomic(String id, long balance) {
    this.id = id;
    this.balance = new AtomicLong(balance);
  }

  public long getBalance() {
    return balance.get();
  }

  public void credit(final long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Negative amounts are not allowed");
    }
    balance.addAndGet(amount);
  }

  public void debit(final long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Negative amounts are not allowed");
    }
    long currentBalance;

    do {
      currentBalance = balance.get();
      if (currentBalance < amount) throw new InsufficientFundsException(id);
    } while (!balance.compareAndSet(currentBalance, currentBalance - amount));
  }
}

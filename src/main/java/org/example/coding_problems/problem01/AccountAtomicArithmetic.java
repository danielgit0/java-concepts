package org.example.coding_problems.problem01;

import java.util.concurrent.atomic.AtomicLong;

public class AccountAtomicArithmetic {
  private final String id;
  private AtomicLong balance;

  public AccountAtomicArithmetic(String id, long balance) {
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

    long currentBalance;
    long newBalance;
    do {
      currentBalance = balance.get();
      newBalance = Math.addExact(currentBalance, amount);
    } while (!balance.compareAndSet(currentBalance, newBalance));
  }

  public void debit(final long amount) {
    if (amount <= 0) {
      throw new IllegalArgumentException("Negative amounts are not allowed");
    }

    long currentBalance;
    long newBalance;
    do {
      currentBalance = balance.get();
      if (currentBalance < amount) throw new InsufficientFundsException(id);
      newBalance = currentBalance - amount;
    } while (!balance.compareAndSet(currentBalance, newBalance));
  }
}

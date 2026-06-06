package org.example.coding_problems.problem01;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;
import org.example.coding_problems.problem01.BalanceEvent.EventType;

public class AccountAtomicArithmeticWithEvents {

  private final String id;
  private final AtomicLong balance;
  private final List<BalanceEvent> events = new CopyOnWriteArrayList<>();

  public AccountAtomicArithmeticWithEvents(String id, long balance) {
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

    events.add(
        new BalanceEvent(
            UUID.randomUUID().toString(),
            EventType.CREDIT,
            currentBalance,
            amount,
            newBalance,
            Instant.now()));
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

    events.add(
        new BalanceEvent(
            UUID.randomUUID().toString(),
            EventType.DEBIT,
            currentBalance,
            amount,
            newBalance,
            Instant.now()));
  }

  public List<BalanceEvent> drainEvents() {
    List<BalanceEvent> snapshot = List.copyOf(events);
    events.clear();
    return snapshot;
  }
}

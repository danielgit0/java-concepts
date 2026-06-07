package org.example.coding_problems.problem02;

import java.util.concurrent.TimeUnit;

public class TransferServiceImpl implements TransferService {

  @Override
  public void transfer(Account from, Account to, long amount) {
    if (from.equals(to)) {
      throw new IllegalArgumentException("Transfer to the same account is not allowed");
    }
    Account first = from.getId().compareTo(to.getId()) < 0 ? from : to;
    Account second = from.getId().compareTo(to.getId()) < 0 ? to : from;

    synchronized (first) {
      synchronized (second) {
        from.debit(amount);
        to.credit(amount);
      }
    }
  }

  @Override
  public void transfer(AccountReentrantLock from, AccountReentrantLock to, long amount) {
    if (from.equals(to)) {
      throw new IllegalArgumentException("Transfer to the same account is not allowed");
    }
    AccountReentrantLock first = from.getId().compareTo(to.getId()) < 0 ? from : to;
    AccountReentrantLock second = from.getId().compareTo(to.getId()) < 0 ? to : from;

    first.getLock().lock();
    try {
      second.getLock().lock();
      try {
        from.debit(amount);
        to.credit(amount);
      } finally {
        second.getLock().unlock();
      }
    } finally {
      first.getLock().unlock();
    }
  }

  @Override
  public void transferTryLock(AccountReentrantLock from, AccountReentrantLock to, long amount) {
    if (from.equals(to)) {
      throw new IllegalArgumentException("Transfer to the same account is not allowed");
    }

    boolean fromLocked = false, toLocked = false;

    try {
      fromLocked = from.getLock().tryLock(100, TimeUnit.MILLISECONDS);
      toLocked = to.getLock().tryLock(100, TimeUnit.MILLISECONDS);

      if (!fromLocked || !toLocked) {
        throw new TransferConflictException("");
      }
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    } finally {
      if (fromLocked) {
        from.getLock().unlock();
      }
      if (toLocked) {
        to.getLock().unlock();
      }
    }
  }
}

package org.example.coding_problems.problem02;

public interface TransferService {
  void transfer(Account from, Account to, long amount);

  void transfer(AccountReentrantLock from, AccountReentrantLock to, long amount);

  void transferTryLock(AccountReentrantLock from, AccountReentrantLock to, long amount);
}

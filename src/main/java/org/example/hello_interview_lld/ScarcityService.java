package org.example.hello_interview_lld;

import static java.lang.IO.println;

import java.util.concurrent.BlockingDeque;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.Semaphore;

public class ScarcityService {

  Semaphore permits;
  BlockingDeque<Scarcity> pool;

  public ScarcityService() throws InterruptedException {
    permits = new Semaphore(5);
    pool = new LinkedBlockingDeque<>(10);
    for (int i = 0; i < 10; i++) {
      pool.put(new Scarcity(i));
    }
  }

  public void helloSemaphore() {
    try {
      permits.acquire();
      println("Hello from thread: %s".formatted(Thread.currentThread()));
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    } finally {
      permits.release();
    }
  }

  public void helloBlockingQueue() throws InterruptedException {
    Scarcity scarcity = pool.take();
    try {
      println("Hello scarcity %s from thread: %s".formatted(scarcity.id(), Thread.currentThread()));
    } finally {
      pool.put(scarcity);
    }
  }
}

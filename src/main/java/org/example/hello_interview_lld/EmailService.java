package org.example.hello_interview_lld;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class EmailService {

  BlockingQueue<Email> queue = new LinkedBlockingQueue<>(10);

  public void publishEmail(Email email) {
    try {
      queue.put(email);
    } catch (InterruptedException e) {
      throw new RuntimeException(e);
    }
  }

  void consume() throws InterruptedException {
    Email email = queue.take();
    sendEmail(email);
  }

  private void sendEmail(Email email) throws InterruptedException {
    Thread.sleep(100);
  }
}

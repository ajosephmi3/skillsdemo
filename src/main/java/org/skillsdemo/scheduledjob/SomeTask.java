package org.skillsdemo.scheduledjob;

import org.springframework.stereotype.Component;

@Component
public class SomeTask {

  // @Scheduled(cron = "0 15 10 15 * ?")
  // @Scheduled(fixedDelay = 1000)
  public void cronTask() {
    System.out.println("scheduled task - " + System.currentTimeMillis() / 1000);
  }
}

package com.sorinwork.tickets.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.stereotype.Component;

/** The limit on checkout starts per visitor, as a bean so tests can reset it. */
@Component
public class CheckoutLimits {

  private final RateLimiter starts;

  public CheckoutLimits(AppProperties properties, Clock clock) {
    this.starts =
        new RateLimiter(properties.checkout().startsPerMinute(), Duration.ofMinutes(1), clock);
  }

  public boolean mayStart(String clientKey) {
    return starts.tryAcquire(clientKey);
  }

  public void clear() {
    starts.clear();
  }
}

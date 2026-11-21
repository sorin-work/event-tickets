package com.sorinwork.tickets.config;

import java.time.Clock;
import java.time.Duration;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** The limit on checkout starts per visitor and event, as a bean so tests can reset it. */
@Component
public class CheckoutLimits {

  static final int MAX_KEYS = 10_000;

  private final RateLimiter starts;

  public CheckoutLimits(AppProperties properties, Clock clock) {
    this.starts =
        new RateLimiter(
            properties.checkout().startsPerMinute(), Duration.ofMinutes(1), MAX_KEYS, clock);
  }

  /** Whether the visitor may start another checkout for the event; null keys are not limited. */
  public boolean mayStart(long eventId, String clientKey) {
    return starts.tryAcquire(clientKey == null ? null : eventId + ":" + clientKey);
  }

  @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
  public void sweep() {
    starts.sweep();
  }

  public void clear() {
    starts.clear();
  }
}

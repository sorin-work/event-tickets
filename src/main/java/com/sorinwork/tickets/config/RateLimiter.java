package com.sorinwork.tickets.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Counts attempts per key in fixed windows, in memory. Per process, which suits the single instance
 * the app is run as; a restart forgets every count.
 */
public class RateLimiter {

  /** Above this many keys, windows that are over are dropped so the map cannot grow forever. */
  private static final int SWEEP_ABOVE = 10_000;

  private final int limit;
  private final Duration window;
  private final Clock clock;
  private final Map<String, Window> windows = new ConcurrentHashMap<>();

  private record Window(Instant start, int count) {}

  /**
   * @param limit attempts allowed per key and window; 0 allows everything
   */
  public RateLimiter(int limit, Duration window, Clock clock) {
    this.limit = limit;
    this.window = window;
    this.clock = clock;
  }

  /** Records an attempt and says whether it is within the limit. */
  public boolean tryAcquire(String key) {
    if (limit <= 0 || key == null) {
      return true;
    }
    Instant now = clock.instant();
    if (windows.size() > SWEEP_ABOVE) {
      windows.values().removeIf(w -> !now.isBefore(w.start().plus(window)));
    }
    Window updated =
        windows.compute(
            key,
            (k, w) ->
                w == null || !now.isBefore(w.start().plus(window))
                    ? new Window(now, 1)
                    : new Window(w.start(), w.count() + 1));
    return updated.count() <= limit;
  }

  /** Forgets every count, for tests. */
  public void clear() {
    windows.clear();
  }
}

package com.sorinwork.tickets.config;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Counts attempts per key in fixed windows, in memory. Per process, which suits the single instance
 * the app is run as; a restart forgets every count.
 *
 * <p>The map is bounded: windows are kept in the order they started, {@link #sweep} drops the ones
 * that are over (called on a timer), and when the map is full the oldest window is evicted. A burst
 * of new keys can therefore never grow it past {@code maxKeys}; at worst it forgets the oldest
 * counts early.
 */
public class RateLimiter {

  private final int limit;
  private final Duration window;
  private final int maxKeys;
  private final Clock clock;

  /** Oldest window first: a key that starts a new window is moved to the end. */
  private final LinkedHashMap<String, Window> windows = new LinkedHashMap<>();

  private record Window(Instant start, int count) {}

  /**
   * @param limit attempts allowed per key and window; 0 allows everything
   * @param maxKeys most keys kept at once
   */
  public RateLimiter(int limit, Duration window, int maxKeys, Clock clock) {
    this.limit = limit;
    this.window = window;
    this.maxKeys = maxKeys;
    this.clock = clock;
  }

  /** Records an attempt and says whether it is within the limit. */
  public synchronized boolean tryAcquire(String key) {
    if (limit <= 0 || key == null) {
      return true;
    }
    Instant now = clock.instant();
    Window current = windows.get(key);
    if (current != null && now.isBefore(current.start().plus(window))) {
      Window counted = new Window(current.start(), current.count() + 1);
      windows.put(key, counted);
      return counted.count() <= limit;
    }
    windows.remove(key);
    if (windows.size() >= maxKeys) {
      Iterator<String> oldest = windows.keySet().iterator();
      oldest.next();
      oldest.remove();
    }
    windows.put(key, new Window(now, 1));
    return true;
  }

  /** Drops windows that are over. Cheap: it stops at the first window still running. */
  public synchronized void sweep() {
    Instant now = clock.instant();
    Iterator<Map.Entry<String, Window>> entries = windows.entrySet().iterator();
    while (entries.hasNext()) {
      if (now.isBefore(entries.next().getValue().start().plus(window))) {
        return;
      }
      entries.remove();
    }
  }

  synchronized int size() {
    return windows.size();
  }

  /** Forgets every count, for tests. */
  public synchronized void clear() {
    windows.clear();
  }
}

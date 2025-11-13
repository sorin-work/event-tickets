package com.sorinwork.tickets.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** A clock tests can move forward, e.g. to let checkout holds run out. */
public class TestClock extends Clock {

  public static final Instant START = Instant.parse("2030-01-15T10:00:00Z");

  private Instant now = START;

  public void reset() {
    now = START;
  }

  public void set(Instant instant) {
    now = instant;
  }

  public void advance(Duration duration) {
    now = now.plus(duration);
  }

  @Override
  public ZoneId getZone() {
    return ZoneOffset.UTC;
  }

  @Override
  public Clock withZone(ZoneId zone) {
    return Clock.fixed(now, zone);
  }

  @Override
  public Instant instant() {
    return now;
  }
}

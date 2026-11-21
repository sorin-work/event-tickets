package com.sorinwork.tickets.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.support.TestClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

  TestClock clock = new TestClock();

  @Test
  void allowsUpToTheLimitPerWindow() {
    RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), clock);

    assertThat(limiter.tryAcquire("a")).isTrue();
    assertThat(limiter.tryAcquire("a")).isTrue();
    assertThat(limiter.tryAcquire("a")).isTrue();
    assertThat(limiter.tryAcquire("a")).isFalse();
    assertThat(limiter.tryAcquire("b")).isTrue();
  }

  @Test
  void aNewWindowStartsAfresh() {
    RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), clock);
    limiter.tryAcquire("a");
    assertThat(limiter.tryAcquire("a")).isFalse();

    clock.advance(Duration.ofMinutes(1));

    assertThat(limiter.tryAcquire("a")).isTrue();
  }

  @Test
  void zeroMeansNoLimit() {
    RateLimiter limiter = new RateLimiter(0, Duration.ofMinutes(1), clock);

    for (int i = 0; i < 100; i++) {
      assertThat(limiter.tryAcquire("a")).isTrue();
    }
  }

  @Test
  void clientKeysAreStableAndDoNotContainTheAddress() {
    ClientKeys keys = new ClientKeys();

    assertThat(keys.of("203.0.113.7")).isEqualTo(keys.of("203.0.113.7")).hasSize(64);
    assertThat(keys.of("203.0.113.7")).doesNotContain("203").isNotEqualTo(keys.of("203.0.113.8"));
    assertThat(keys.of(null)).isNull();
  }
}

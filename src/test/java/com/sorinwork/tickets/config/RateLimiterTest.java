package com.sorinwork.tickets.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.support.TestClock;
import java.time.Duration;
import org.junit.jupiter.api.Test;

class RateLimiterTest {

  TestClock clock = new TestClock();

  @Test
  void allowsUpToTheLimitPerWindow() {
    RateLimiter limiter = new RateLimiter(3, Duration.ofMinutes(1), 10_000, clock);

    assertThat(limiter.tryAcquire("a")).isTrue();
    assertThat(limiter.tryAcquire("a")).isTrue();
    assertThat(limiter.tryAcquire("a")).isTrue();
    assertThat(limiter.tryAcquire("a")).isFalse();
    assertThat(limiter.tryAcquire("b")).isTrue();
  }

  @Test
  void aNewWindowStartsAfresh() {
    RateLimiter limiter = new RateLimiter(1, Duration.ofMinutes(1), 10_000, clock);
    limiter.tryAcquire("a");
    assertThat(limiter.tryAcquire("a")).isFalse();

    clock.advance(Duration.ofMinutes(1));

    assertThat(limiter.tryAcquire("a")).isTrue();
  }

  @Test
  void zeroMeansNoLimit() {
    RateLimiter limiter = new RateLimiter(0, Duration.ofMinutes(1), 10_000, clock);

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

  @Test
  void aBurstOfNewKeysNeverGrowsTheMapPastItsCap() {
    RateLimiter limiter = new RateLimiter(5, Duration.ofMinutes(1), 100, clock);

    for (int i = 0; i < 5_000; i++) {
      limiter.tryAcquire("key-" + i);
      assertThat(limiter.size()).isLessThanOrEqualTo(100);
    }
    // The newest keys are the ones kept.
    for (int i = 0; i < 4; i++) {
      limiter.tryAcquire("key-4999");
    }
    assertThat(limiter.tryAcquire("key-4999")).isFalse();
  }

  @Test
  void theSweepDropsWindowsThatAreOver() {
    RateLimiter limiter = new RateLimiter(5, Duration.ofMinutes(1), 100, clock);
    limiter.tryAcquire("early");
    clock.advance(Duration.ofSeconds(30));
    limiter.tryAcquire("later");
    clock.advance(Duration.ofSeconds(40));

    limiter.sweep();

    assertThat(limiter.size()).isEqualTo(1);
  }

  @Test
  void ipv6AddressesAreKeyedByTheirSlash64() {
    ClientKeys keys = new ClientKeys();

    assertThat(keys.of("2001:db8:1:2::10")).isEqualTo(keys.of("2001:db8:1:2:abcd::99"));
    assertThat(keys.of("2001:db8:1:2::10")).isNotEqualTo(keys.of("2001:db8:1:3::10"));
    assertThat(keys.of("[2001:db8:1:2::10]")).isEqualTo(keys.of("2001:db8:1:2::77"));
  }

  @Test
  void mappedIpv4CountsAsIpv4() {
    ClientKeys keys = new ClientKeys();

    assertThat(keys.of("::ffff:203.0.113.7")).isEqualTo(keys.of("203.0.113.7"));
    assertThat(keys.of("::ffff:203.0.113.7")).isNotEqualTo(keys.of("203.0.113.8"));
  }

  @Test
  void textThatIsNotAnAddressIsStillKeyed() {
    ClientKeys keys = new ClientKeys();

    assertThat(keys.of("unix:socket")).hasSize(64).isEqualTo(keys.of("unix:socket"));
  }
}

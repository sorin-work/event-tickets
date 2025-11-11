package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TierAvailabilityTest {

  @Test
  void remainingSubtractsSoldAndHeld() {
    assertThat(new TierAvailability(1, 100, 30, 5).remaining()).isEqualTo(65);
  }

  @Test
  void neverGoesBelowZero() {
    TierAvailability availability = new TierAvailability(1, 10, 12, 0);

    assertThat(availability.remaining()).isZero();
    assertThat(availability.isSoldOut()).isTrue();
  }
}

package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HoldExpiryServiceTest extends IntegrationTest {

  @Autowired HoldExpiryService expiry;
  @Autowired CheckoutService checkout;
  @Autowired EventService events;
  @Autowired OrderRepository orders;

  @Test
  void expiresOnlyHoldsThatRanOut() {
    Event event = data.event("Talk");
    TicketTier tier = data.tier(event, "Seat", "0", 20);
    events.publish(event.getId());
    Order old = checkout.start(event.getId(), Map.of(tier.getId(), 1));
    Order paid = checkout.start(event.getId(), Map.of(tier.getId(), 1));
    checkout.complete(paid.getAccessToken(), new CheckoutForm("Ana", "ana@example.com"));
    clock.advance(Duration.ofMinutes(6));
    Order fresh = checkout.start(event.getId(), Map.of(tier.getId(), 1));
    clock.advance(Duration.ofMinutes(5));

    assertThat(expiry.expireHolds()).isEqualTo(1);

    assertThat(orders.findById(old.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.EXPIRED);
    assertThat(orders.findById(fresh.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.PENDING);
    assertThat(orders.findById(paid.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
  }

  @Test
  void doesNothingWithoutStaleHolds() {
    assertThat(expiry.expireHolds()).isZero();
  }
}

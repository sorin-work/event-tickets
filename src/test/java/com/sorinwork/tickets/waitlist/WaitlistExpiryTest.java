package com.sorinwork.tickets.waitlist;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.CheckoutForm;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.HoldExpiryService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WaitlistExpiryTest extends IntegrationTest {

  @Autowired WaitlistService waitlist;
  @Autowired WaitlistRepository entries;
  @Autowired EventService events;
  @Autowired CheckoutService checkout;
  @Autowired HoldExpiryService expiry;

  @Test
  void seatsFromAnAbandonedCheckoutGoToTheWaitlist() {
    Event event = data.event("Small Gig");
    TicketTier tier = data.tier(event, "Floor", "15", 1);
    event = events.publish(event.getId());
    Order abandoned = checkout.start(event.getId(), Map.of(tier.getId(), 1));
    WaitlistEntry ana =
        waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", "ana@example.com", 1));

    clock.advance(Duration.ofMinutes(11));
    expiry.expireHolds();

    assertThat(entries.findById(ana.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.OFFERED);
    assertThat(abandoned.getId()).isNotNull();
  }

  @Test
  void anOfferThatRunsOutIsMarkedExpired() {
    Event event = data.event("Small Gig");
    TicketTier tier = data.tier(event, "Floor", "15", 1);
    event = events.publish(event.getId());
    checkout.start(event.getId(), Map.of(tier.getId(), 1));
    WaitlistEntry ana =
        waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", "ana@example.com", 1));
    clock.advance(Duration.ofMinutes(11));
    expiry.expireHolds();

    clock.advance(Duration.ofHours(25));
    expiry.expireHolds();

    assertThat(entries.findById(ana.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.EXPIRED);
  }

  @Test
  void buyingTheOfferedTicketsClaimsTheOffer() {
    Event event = data.event("Small Gig");
    TicketTier tier = data.tier(event, "Floor", "15", 1);
    event = events.publish(event.getId());
    checkout.start(event.getId(), Map.of(tier.getId(), 1));
    WaitlistEntry ana =
        waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", "ana@example.com", 1));
    clock.advance(Duration.ofMinutes(11));
    expiry.expireHolds();
    String token =
        entries.findWithOfferById(ana.getId()).orElseThrow().getOfferOrder().getAccessToken();

    clock.advance(Duration.ofHours(3));
    checkout.complete(token, new CheckoutForm("Ana Pop", "ana@example.com"));

    assertThat(entries.findById(ana.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.CLAIMED);
  }
}

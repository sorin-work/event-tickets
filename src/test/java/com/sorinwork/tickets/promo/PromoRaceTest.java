package com.sorinwork.tickets.promo;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.PostgresTest;
import com.sorinwork.tickets.support.Race;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Buyers applying the last uses of a code at the same moment, on PostgreSQL. */
class PromoRaceTest extends PostgresTest {

  @Autowired CheckoutService checkout;
  @Autowired EventService events;
  @Autowired PromoCodeRepository promos;

  @Test
  void aLimitedCodeIsNeverUsedMoreOftenThanAllowed() throws Exception {
    Event event = data.event("Synth Workshop");
    TicketTier tier = data.tier(event, "Seat", "40", 50);
    events.publish(event.getId());
    PromoCode promo = new PromoCode(event, "LAST2", DiscountType.PERCENT, 50, clock.instant());
    promo.setMaxUses(2);
    promo = promos.save(promo);

    List<Callable<Order>> appliers = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String token = checkout.start(event.getId(), Map.of(tier.getId(), 1)).getAccessToken();
      appliers.add(() -> checkout.applyPromoCode(token, "LAST2"));
    }

    List<Race.Outcome<Order>> outcomes = Race.run(appliers);

    assertThat(outcomes.stream().filter(Race.Outcome::succeeded)).hasSize(2);
    assertThat(outcomes.stream().filter(o -> !o.succeeded()))
        .allSatisfy(o -> assertThat(o.error()).hasMessage("That promo code has been used up."));
    assertThat(promos.countRedemptions(promo.getId(), clock.instant())).isEqualTo(2);
  }
}

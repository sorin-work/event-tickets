package com.sorinwork.tickets.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.TierAvailability;
import com.sorinwork.tickets.support.PostgresTest;
import com.sorinwork.tickets.support.Race;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** An admin lowering a tier or the venue while buyers check out, on PostgreSQL. */
class SeatChangeRaceTest extends PostgresTest {

  @Autowired EventService events;
  @Autowired TierService tiers;
  @Autowired CheckoutService checkout;
  @Autowired AvailabilityService availability;

  @Test
  void loweringATierNeverLeavesMoreTakenThanItHas() throws Exception {
    Event event = data.event("Small Gig");
    TicketTier tier = data.tier(event, "Floor", "15", 10);
    events.publish(event.getId());

    List<Callable<Object>> calls = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      calls.add(() -> checkout.start(event.getId(), Map.of(tier.getId(), 1)));
    }
    calls.add(
        () -> {
          TierForm form = TierForm.from(tiers.get(event.getId(), tier.getId()));
          form.setQuantity(5);
          return tiers.update(event.getId(), tier.getId(), form);
        });

    List<Race.Outcome<Object>> outcomes = Race.run(calls);

    assertThat(outcomes)
        .allSatisfy(
            o -> {
              if (!o.succeeded()) {
                assertThat(o.error()).isInstanceOf(BusinessException.class);
              }
            });
    int quantity = tiers.get(event.getId(), tier.getId()).getQuantity();
    TierAvailability now = availability.forEvent(event.getId()).get(tier.getId());
    assertThat(now.sold() + now.held()).isLessThanOrEqualTo(quantity);
  }

  @Test
  void loweringTheVenueNeverLeavesMoreTakenThanItHolds() throws Exception {
    Event event = data.event("Basement Show");
    TicketTier standing = data.tier(event, "Standing", "10", 10);
    TicketTier seated = data.tier(event, "Seated", "15", 10);
    events.publish(event.getId());

    List<Callable<Object>> calls = new ArrayList<>();
    for (int i = 0; i < 8; i++) {
      TicketTier tier = i % 2 == 0 ? standing : seated;
      calls.add(() -> checkout.start(event.getId(), Map.of(tier.getId(), 1)));
    }
    calls.add(
        () -> {
          EventForm form = EventForm.from(events.get(event.getId()));
          form.setCapacity(5);
          return events.update(event.getId(), form);
        });

    List<Race.Outcome<Object>> outcomes = Race.run(calls);

    assertThat(outcomes)
        .allSatisfy(
            o -> {
              if (!o.succeeded()) {
                assertThat(o.error()).isInstanceOf(BusinessException.class);
              }
            });
    Integer capacity = events.get(event.getId()).getCapacity();
    int taken =
        availability.forEvent(event.getId()).values().stream()
            .mapToInt(a -> a.sold() + a.held())
            .sum();
    if (capacity != null) {
      assertThat(taken).isLessThanOrEqualTo(capacity);
    }
  }
}

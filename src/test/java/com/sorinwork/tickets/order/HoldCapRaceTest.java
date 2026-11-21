package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.PostgresTest;
import com.sorinwork.tickets.support.Race;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** One visitor firing several checkouts at once, on PostgreSQL. */
class HoldCapRaceTest extends PostgresTest {

  @Autowired CheckoutService checkout;
  @Autowired EventService events;
  @Autowired OrderItemRepository items;

  @Test
  void parallelRequestsCannotTogetherPassTheCap() throws Exception {
    Event event = data.event("Popular Night");
    TicketTier tier = data.tier(event, "Floor", "20", 100);
    events.publish(event.getId());
    checkout.start(event.getId(), Map.of(tier.getId(), 4), null, "script");

    List<Callable<Order>> requests = new ArrayList<>();
    for (int i = 0; i < 6; i++) {
      requests.add(() -> checkout.start(event.getId(), Map.of(tier.getId(), 4), null, "script"));
    }
    List<Race.Outcome<Order>> outcomes = Race.run(requests);

    // 4 held, room for 8 more under the default cap of 12: exactly two of six get through.
    assertThat(outcomes.stream().filter(Race.Outcome::succeeded)).hasSize(2);
    assertThat(items.heldByClient(event.getId(), "script", clock.instant(), OrderStatus.PENDING))
        .isEqualTo(12);
  }
}

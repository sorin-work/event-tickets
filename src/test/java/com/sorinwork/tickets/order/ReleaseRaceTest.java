package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.PostgresTest;
import com.sorinwork.tickets.support.Race;
import com.sorinwork.tickets.ticket.TicketRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Giving seats back while paying for them, or from two tabs at once, on PostgreSQL. */
class ReleaseRaceTest extends PostgresTest {

  @Autowired CheckoutService checkout;
  @Autowired EventService events;
  @Autowired OrderRepository orders;
  @Autowired TicketRepository tickets;
  @Autowired AvailabilityService availability;

  @Test
  void anOrderIsEitherPaidWithTicketsOrReleasedWithout() throws Exception {
    Event event = data.event("Film Club");
    TicketTier tier = data.tier(event, "Seat", "6", 50);
    events.publish(event.getId());

    for (int round = 0; round < 10; round++) {
      String token = checkout.start(event.getId(), Map.of(tier.getId(), 2)).getAccessToken();
      List<Callable<Object>> calls =
          List.of(
              () -> checkout.complete(token, new CheckoutForm("Ana", "ana@example.com")),
              () -> checkout.release(token));

      Race.run(calls);

      Order order = orders.findByAccessToken(token).orElseThrow();
      assertThat(order.getStatus()).isIn(OrderStatus.PAID, OrderStatus.EXPIRED);
      assertThat(tickets.findByOrderIdOrderById(order.getId()))
          .hasSize(order.getStatus() == OrderStatus.PAID ? 2 : 0);
    }
  }

  @Test
  void twoTabsStartingAgainNeverHoldMoreThanThereIs() throws Exception {
    Event event = data.event("Small Gig");
    TicketTier tier = data.tier(event, "Floor", "15", 4);
    events.publish(event.getId());
    String first = checkout.start(event.getId(), Map.of(tier.getId(), 4)).getAccessToken();

    List<Callable<Order>> tabs = new ArrayList<>();
    for (int i = 0; i < 2; i++) {
      tabs.add(() -> checkout.start(event.getId(), Map.of(tier.getId(), 3), first));
    }
    List<Race.Outcome<Order>> outcomes = Race.run(tabs);

    // One tab replaces the first checkout; the other finds only one seat left and is refused.
    assertThat(outcomes.stream().filter(Race.Outcome::succeeded)).hasSize(1);
    assertThat(orders.findByAccessToken(first).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.EXPIRED);
    assertThat(availability.forEvent(event.getId()).get(tier.getId()).held()).isEqualTo(3);
  }
}

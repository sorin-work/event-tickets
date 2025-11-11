package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.common.Tokens;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class AvailabilityServiceTest extends IntegrationTest {

  @Autowired AvailabilityService availability;
  @Autowired OrderRepository orders;

  private void order(Event event, TicketTier tier, int qty, OrderStatus status, Instant expires) {
    Order order =
        new Order(event, Tokens.orderReference(), Tokens.accessToken(), Instant.now(), expires);
    order.setStatus(status);
    order.addItem(new OrderItem(order, tier, qty));
    orders.save(order);
  }

  @Test
  void countsPaidOrdersAndActiveHolds() {
    Event event = data.event("Jazz");
    TicketTier standing = data.tier(event, "Standing", "10", 50);
    TicketTier seated = data.tier(event, "Seated", "20", 20);
    Instant later = Instant.now().plus(Duration.ofMinutes(5));
    Instant earlier = Instant.now().minus(Duration.ofMinutes(5));

    order(event, standing, 3, OrderStatus.PAID, earlier);
    order(event, standing, 2, OrderStatus.PENDING, later);
    order(event, standing, 4, OrderStatus.PENDING, earlier);
    order(event, standing, 1, OrderStatus.EXPIRED, earlier);
    order(event, seated, 20, OrderStatus.PAID, earlier);

    Map<Long, TierAvailability> result = availability.forEvent(event.getId());

    assertThat(result.get(standing.getId()))
        .isEqualTo(new TierAvailability(standing.getId(), 50, 3, 2));
    assertThat(result.get(standing.getId()).remaining()).isEqualTo(45);
    assertThat(result.get(seated.getId()).isSoldOut()).isTrue();
  }

  @Test
  void tiersWithoutOrdersAreFullyAvailable() {
    Event event = data.event("Jazz");
    TicketTier tier = data.tier(event, "Standing", "10", 50);

    assertThat(availability.forEvent(event.getId()).get(tier.getId()).remaining()).isEqualTo(50);
  }
}

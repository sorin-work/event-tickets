package com.sorinwork.tickets.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.notify.OutboxRepository;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.order.OrderStatus;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.waitlist.WaitlistForm;
import com.sorinwork.tickets.waitlist.WaitlistService;
import com.sorinwork.tickets.waitlist.WaitlistStatus;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventCancellationTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired CheckoutService checkout;
  @Autowired OrderAdminService orders;
  @Autowired OutboxRepository outbox;
  @Autowired WaitlistService waitlist;

  @Test
  void cancellingAnEventCancelsItsOrdersAndEmailsBuyers() {
    Event event = data.event("Rained Off");
    TicketTier tier = data.tier(event, "Field", "10", 3);
    event = events.publish(event.getId());
    Order paid = purchases.buy(tier, 2, "Ana", "ana@example.com");
    Order open = checkout.start(event.getId(), Map.of(tier.getId(), 1));
    var entry = waitlist.join(event, new WaitlistForm(tier.getId(), "Bob", "bob@example.com", 1));

    events.cancel(event.getId());

    assertThat(orders.get(paid.getId()).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(orders.get(open.getId()).getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(outbox.findByRecipientOrderByIdAsc("ana@example.com"))
        .extracting(m -> m.getSubject())
        .contains("Order cancelled: Rained Off");
    assertThat(waitlist.forEvent(event.getId()))
        .singleElement()
        .satisfies(e -> assertThat(e.getStatus()).isEqualTo(WaitlistStatus.WAITING));
    assertThat(entry.getId()).isNotNull();
  }

  @Test
  void cancellingTwiceDoesNothingMore() {
    Event event = data.event("Rained Off");
    TicketTier tier = data.tier(event, "Field", "10", 3);
    events.publish(event.getId());
    purchases.buy(tier, 1, "Ana", "ana@example.com");
    events.cancel(event.getId());

    events.cancel(event.getId());

    assertThat(outbox.findByRecipientOrderByIdAsc("ana@example.com")).hasSize(2);
  }
}

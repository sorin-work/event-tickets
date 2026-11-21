package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.CheckInService;
import com.sorinwork.tickets.ticket.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckInMessagesTest extends IntegrationTest {

  @Autowired CheckInMessages messages;
  @Autowired CheckInService checkIn;
  @Autowired EventService events;
  @Autowired OrderAdminService orders;
  @Autowired TicketRepository tickets;

  Event event;
  Order order;
  String code;

  @BeforeEach
  void setUp() {
    event = data.event("Gig & Talk");
    TicketTier tier = data.tier(event, "Floor", "10", 10);
    event = events.publish(event.getId());
    order = purchases.buy(tier, 1, "Ana Pop", "ana@example.com");
    code = tickets.findByOrderIdOrderById(order.getId()).get(0).getCode();
  }

  private String scan(Event at) {
    return messages.html(checkIn.checkIn(at.getId(), code, "door"), at);
  }

  @Test
  void admitted() {
    assertThat(scan(event)).isEqualTo("<strong>Ana Pop</strong> · Floor");
  }

  @Test
  void alreadyIn() {
    scan(event);

    assertThat(scan(event))
        .isEqualTo("<strong>Ana Pop</strong> already checked in at Tue 15 Jan 2030, 12:00 by door");
  }

  @Test
  void wrongEventIsNamedAndEscaped() {
    Event other = data.event("Other");

    assertThat(scan(other)).isEqualTo("This ticket is for <strong>Gig &amp; Talk</strong>");
  }

  @Test
  void cancelledAndUnknown() {
    orders.cancel(order.getId());

    assertThat(scan(event)).isEqualTo("The order for this ticket was cancelled");
    assertThat(messages.html(checkIn.checkIn(event.getId(), "ZZZZ", "door"), event))
        .isEqualTo("No ticket with this code");
  }
}

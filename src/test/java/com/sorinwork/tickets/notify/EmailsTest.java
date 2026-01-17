package com.sorinwork.tickets.notify;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EmailsTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired OutboxRepository outbox;

  @Test
  void paidOrdersGetAConfirmationWithTheTicketsLink() {
    Event event = data.event("Salsa Night");
    TicketTier tier = data.tier(event, "Dancer", "12", 40);
    events.publish(event.getId());

    Order order = purchases.buy(tier, 2, "Ana Pop", "Ana@Example.com");

    OutboxMessage message = outbox.findByRecipientOrderByIdAsc("ana@example.com").get(0);
    assertThat(message.getSubject()).isEqualTo("Your tickets for Salsa Night");
    assertThat(message.getBody())
        .contains("Hi Ana Pop,")
        .contains(order.getReference())
        .contains("2 × Dancer")
        .contains("Total: €24.00")
        .contains("http://localhost:8080/orders/" + order.getAccessToken());
    assertThat(message.getEventId()).isEqualTo(event.getId());
  }
}

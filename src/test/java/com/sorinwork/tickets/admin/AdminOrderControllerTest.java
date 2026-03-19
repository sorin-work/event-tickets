package com.sorinwork.tickets.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminOrderControllerTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired CheckoutService checkout;
  @Autowired HoldExpiryService expiry;
  @Autowired com.sorinwork.tickets.notify.OutboxRepository outbox;

  Event event;
  TicketTier tier;

  @BeforeEach
  void setUp() {
    event = data.event("Wine Tasting");
    tier = data.tier(event, "Seat", "35", 20);
    events.publish(event.getId());
  }

  private Order paidOrder(String name) {
    Order order = checkout.start(event.getId(), Map.of(tier.getId(), 2));
    return checkout.complete(order.getAccessToken(), new CheckoutForm(name, "x@example.com"));
  }

  @Test
  void listsOrdersButNotExpiredCheckouts() throws Exception {
    Order paid = paidOrder("Maria Ionescu");
    Order abandoned = checkout.start(event.getId(), Map.of(tier.getId(), 1));
    clock.advance(Duration.ofMinutes(20));
    expiry.expireHolds();

    mvc.perform(get("/admin/events/" + event.getId() + "/orders"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString(paid.getReference())))
        .andExpect(content().string(containsString("Maria Ionescu")))
        .andExpect(content().string(not(containsString(abandoned.getReference()))));
  }

  @Test
  void showsOrderWithTickets() throws Exception {
    Order paid = paidOrder("Maria Ionescu");

    mvc.perform(get("/admin/orders/" + paid.getId()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("2 × Seat")))
        .andExpect(content().string(containsString("€70.00")))
        .andExpect(content().string(containsString("Paid")));
  }

  @Test
  void resendsTheTicketEmail() throws Exception {
    Order paid = paidOrder("Maria Ionescu");

    mvc.perform(
            org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(
                    "/admin/orders/" + paid.getId() + "/resend")
                .with(
                    org.springframework.security.test.web.servlet.request
                        .SecurityMockMvcRequestPostProcessors.csrf()))
        .andExpect(status().is3xxRedirection());

    org.assertj.core.api.Assertions.assertThat(outbox.findByRecipientOrderByIdAsc("x@example.com"))
        .hasSize(2);
  }

  @Test
  void emptyState() throws Exception {
    mvc.perform(get("/admin/events/" + event.getId() + "/orders"))
        .andExpect(content().string(containsString("No orders yet.")));
  }

  @Test
  void unknownOrderIs404() throws Exception {
    mvc.perform(get("/admin/orders/999")).andExpect(status().isNotFound());
  }
}

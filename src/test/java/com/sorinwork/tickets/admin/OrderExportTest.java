package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class OrderExportTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired OrderAdminService orders;
  @Autowired CheckoutService checkout;

  @Test
  void exportsPaidAndCancelledOrdersOldestFirst() throws Exception {
    Event event = data.event("Wine Tasting");
    TicketTier tier = data.tier(event, "Seat", "35", 20);
    events.publish(event.getId());
    Order first = purchases.buy(tier, 2, "Ana Pop", "ana@example.com");
    clock.advance(java.time.Duration.ofMinutes(5));
    Order second = purchases.buy(tier, 1, "Dan Pop", "dan@example.com");
    orders.cancel(second.getId());
    checkout.start(event.getId(), Map.of(tier.getId(), 1));

    String csv =
        mvc.perform(get("/admin/events/" + event.getId() + "/orders.csv"))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String[] lines = csv.split("\r\n");
    assertThat(lines).hasSize(3);
    assertThat(lines[1])
        .startsWith(first.getReference() + ",PAID,Ana Pop,ana@example.com,2,70.00,0.00,70.00,,");
    assertThat(lines[2]).startsWith(second.getReference() + ",CANCELLED,Dan Pop");
  }
}

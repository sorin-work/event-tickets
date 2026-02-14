package com.sorinwork.tickets.admin;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminDashboardTest extends IntegrationTest {

  @Autowired EventService events;

  @Test
  void showsUpcomingEventsAndRecentOrders() throws Exception {
    Event event = data.event("Garden Party");
    TicketTier tier = data.tier(event, "Guest", "10", 50);
    events.publish(event.getId());
    data.event("Still a Draft");
    Order order = purchases.buy(tier, 1, "Ana Pop", "ana@example.com");

    mvc.perform(get("/admin"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Garden Party")))
        .andExpect(content().string(containsString("Still a Draft")))
        .andExpect(content().string(containsString(order.getReference())));
  }

  @Test
  void emptyDashboard() throws Exception {
    mvc.perform(get("/admin"))
        .andExpect(content().string(containsString("No upcoming events.")))
        .andExpect(content().string(containsString("No orders yet.")));
  }
}

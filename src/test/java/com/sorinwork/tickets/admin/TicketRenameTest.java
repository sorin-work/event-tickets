package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.ticket.TicketRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class TicketRenameTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired TicketRepository tickets;

  @Test
  void renamesATicket() throws Exception {
    Event event = data.event("Wine Tasting");
    TicketTier tier = data.tier(event, "Seat", "35", 20);
    events.publish(event.getId());
    Order order = purchases.buy(tier, 1, "Ana Pop", "ana@example.com");
    Ticket ticket = tickets.findByOrderIdOrderById(order.getId()).get(0);

    mvc.perform(
            post("/admin/tickets/" + ticket.getId() + "/name")
                .with(csrf())
                .param("name", " Dan Pop "))
        .andExpect(redirectedUrl("/admin/orders/" + order.getId()));
    assertThat(tickets.findById(ticket.getId()).orElseThrow().getAttendeeName())
        .isEqualTo("Dan Pop");

    mvc.perform(post("/admin/tickets/" + ticket.getId() + "/name").with(csrf()).param("name", "  "))
        .andExpect(flash().attributeExists("error"));
  }
}

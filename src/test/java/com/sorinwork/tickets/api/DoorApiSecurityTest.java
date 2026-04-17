package com.sorinwork.tickets.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.admin.AdminAccountService;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.ticket.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class DoorApiSecurityTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired TicketRepository tickets;
  @Autowired AdminAccountService accounts;

  Event event;
  Ticket ticket;

  @BeforeEach
  void setUp() {
    accounts.create("door@example.com", "Door", "door-password-1");
    event = data.event("Conference");
    TicketTier tier = data.tier(event, "Pass", "99", 100);
    events.publish(event.getId());
    Order order = purchases.buy(tier, 1, "Ana Pop", "ana@example.com");
    ticket = tickets.findByOrderIdOrderById(order.getId()).get(0);
  }

  private String body() {
    return "{\"code\": \"" + ticket.getCode() + "\"}";
  }

  @Test
  void checkInWithoutCredentialsIsRefused() throws Exception {
    mvc.perform(
            post("/api/v1/admin/events/" + event.getId() + "/check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isUnauthorized());

    assertThat(tickets.findById(ticket.getId()).orElseThrow().isCheckedIn()).isFalse();
  }

  @Test
  void attendeeListWithoutCredentialsIsRefused() throws Exception {
    mvc.perform(get("/api/v1/admin/events/" + event.getId() + "/attendees"))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void wrongPasswordIsRefused() throws Exception {
    mvc.perform(
            get("/api/v1/admin/events/" + event.getId() + "/attendees")
                .with(httpBasic("door@example.com", "nope")))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void scannerAppsSignInWithBasicAuth() throws Exception {
    mvc.perform(
            post("/api/v1/admin/events/" + event.getId() + "/check-in")
                .with(httpBasic("door@example.com", "door-password-1"))
                .contentType(MediaType.APPLICATION_JSON)
                .content(body()))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.admitted").value(true));

    assertThat(tickets.findById(ticket.getId()).orElseThrow().getCheckedInBy())
        .isEqualTo("door@example.com");
  }

  @Test
  void publicEventApiStaysOpen() throws Exception {
    mvc.perform(get("/api/v1/events")).andExpect(status().isOk());
  }
}

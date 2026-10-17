package com.sorinwork.tickets.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "door@example.com", roles = "ADMIN")
class DoorApiControllerTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired TicketRepository tickets;

  Event event;
  String code;

  @BeforeEach
  void setUp() {
    event = data.event("Conference");
    TicketTier tier = data.tier(event, "Pass", "99", 100);
    events.publish(event.getId());
    Order order = purchases.buy(tier, 2, "Ana Pop", "ana@example.com");
    code = tickets.findByOrderIdOrderById(order.getId()).get(0).getCode();
  }

  @Test
  void checksInByCode() throws Exception {
    mvc.perform(
            post("/api/v1/admin/events/" + event.getId() + "/check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"" + code + "\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CHECKED_IN"))
        .andExpect(jsonPath("$.admitted").value(true))
        .andExpect(jsonPath("$.attendee").value("Ana Pop"))
        .andExpect(jsonPath("$.checkedIn").value(1))
        .andExpect(jsonPath("$.total").value(2));
  }

  @Test
  void reportsUnknownCodes() throws Exception {
    mvc.perform(
            post("/api/v1/admin/events/" + event.getId() + "/check-in")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"code\": \"NOPE\"}"))
        .andExpect(jsonPath("$.status").value("NOT_FOUND"))
        .andExpect(jsonPath("$.admitted").value(false));
  }

  @Test
  void listsAttendees() throws Exception {
    mvc.perform(get("/api/v1/admin/events/" + event.getId() + "/attendees"))
        .andExpect(jsonPath("$.total").value(2))
        .andExpect(jsonPath("$.items[0].name").value("Ana Pop"))
        .andExpect(jsonPath("$.items[0].tier").value("Pass"));
  }

  @Test
  void listsOnlyPeopleStillToArrive() throws Exception {
    mvc.perform(
        post("/api/v1/admin/events/" + event.getId() + "/check-in")
            .contentType(MediaType.APPLICATION_JSON)
            .content("{\"code\": \"" + code + "\"}"));

    mvc.perform(get("/api/v1/admin/events/" + event.getId() + "/attendees").param("arrival", "out"))
        .andExpect(jsonPath("$.total").value(1));
    mvc.perform(get("/api/v1/admin/events/" + event.getId() + "/attendees").param("arrival", "in"))
        .andExpect(jsonPath("$.items[0].code").value(code));
  }

  @Test
  void unknownEvent() throws Exception {
    mvc.perform(get("/api/v1/admin/events/999/attendees")).andExpect(status().isNotFound());
  }
}

package com.sorinwork.tickets.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(username = "door@example.com", roles = "ADMIN")
class AdminCheckInControllerTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired TicketRepository tickets;

  Event event;
  String url;
  String code;

  @BeforeEach
  void setUp() {
    event = data.event("Meetup");
    TicketTier tier = data.tier(event, "Standing", "0", 50);
    events.publish(event.getId());
    Order order = purchases.buy(tier, 1, "Ana Pop", "ana@example.com");
    code = tickets.findByOrderIdOrderById(order.getId()).get(0).getCode();
    url = "/admin/events/" + event.getId() + "/check-in";
  }

  @Test
  void showsScanForm() throws Exception {
    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Scan or type the code")));
  }

  @Test
  void htmxScanReturnsJustTheResult() throws Exception {
    mvc.perform(post(url).with(csrf()).header("HX-Request", "true").param("code", code))
        .andExpect(content().string(containsString("Welcome in")))
        .andExpect(content().string(containsString("<strong>Ana Pop</strong> · Standing")))
        .andExpect(content().string(not(containsString("<html"))));
  }

  @Test
  void repeatedScanIsNotAdmitted() throws Exception {
    mvc.perform(post(url).with(csrf()).param("code", code));

    mvc.perform(post(url).with(csrf()).param("code", code))
        .andExpect(content().string(containsString("Not admitted")))
        .andExpect(content().string(containsString("already checked in at")));
  }

  @Test
  void attendeeNamesAreEscaped() throws Exception {
    TicketTier tier = data.tier(event, "VIP", "0", 5);
    Order order = purchases.buy(tier, 1, "<img src=x onerror=alert(1)>", "evil@example.com");
    String evilCode = tickets.findByOrderIdOrderById(order.getId()).get(0).getCode();

    mvc.perform(post(url).with(csrf()).header("HX-Request", "true").param("code", evilCode))
        .andExpect(content().string(containsString("&lt;img src=x onerror=alert(1)&gt;")))
        .andExpect(content().string(not(containsString("<img src=x"))));

    mvc.perform(post(url).with(csrf()).header("HX-Request", "true").param("code", evilCode))
        .andExpect(content().string(not(containsString("<img src=x"))));
  }

  @Test
  void unknownCode() throws Exception {
    mvc.perform(post(url).with(csrf()).param("code", "XXXX"))
        .andExpect(content().string(containsString("No ticket with this code")));
  }

  @Test
  void showsProgressAndUpdatesItAfterAScan() throws Exception {
    mvc.perform(get(url)).andExpect(content().string(containsString("0 / 1")));

    mvc.perform(post(url).with(csrf()).header("HX-Request", "true").param("code", code))
        .andExpect(content().string(containsString("hx-swap-oob=\"true\"")))
        .andExpect(content().string(containsString("1 / 1")));

    mvc.perform(get(url + "/progress"))
        .andExpect(content().string(containsString("1 / 1")))
        .andExpect(content().string(containsString("width:100%")));
  }
}

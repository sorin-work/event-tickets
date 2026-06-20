package com.sorinwork.tickets.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminAttendeeControllerTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired OrderAdminService orders;
  @Autowired com.sorinwork.tickets.ticket.CheckInService checkIn;

  Event event;
  TicketTier tier;
  String url;

  @BeforeEach
  void setUp() {
    event = data.event("Hackathon");
    tier = data.tier(event, "Participant", "0", 200);
    tier.setMaxPerOrder(50);
    data.saveTier(tier);
    events.publish(event.getId());
    url = "/admin/events/" + event.getId() + "/attendees";
  }

  @Test
  void listsTicketHoldersByName() throws Exception {
    purchases.buy(tier, 1, "Zoe Marin", "zoe@example.com");
    purchases.buy(tier, 1, "adrian Pop", "adrian@example.com");

    mvc.perform(get(url))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Showing 1–2 of 2")))
        .andExpect(
            content()
                .string(org.hamcrest.Matchers.stringContainsInOrder("adrian Pop", "Zoe Marin")));
  }

  @Test
  void leavesOutCancelledOrders() throws Exception {
    Order order = purchases.buy(tier, 1, "Gone Person", "gone@example.com");
    orders.cancel(order.getId());

    mvc.perform(get(url)).andExpect(content().string(not(containsString("Gone Person"))));
  }

  @Test
  void pagesThroughAttendees() throws Exception {
    purchases.buy(tier, 50, "Group Buyer", "group@example.com");

    mvc.perform(get(url))
        .andExpect(content().string(containsString("Page 1 of 2")))
        .andExpect(content().string(containsString("Next")));
    mvc.perform(get(url).param("page", "2"))
        .andExpect(content().string(containsString("Showing 26–50 of 50")))
        .andExpect(content().string(containsString("Previous")));
  }

  @Test
  void lastAttendeesOnAPartPageAreReachable() throws Exception {
    purchases.buy(tier, 26, "Group Buyer", "group@example.com");

    mvc.perform(get(url))
        .andExpect(content().string(containsString("Page 1 of 2")))
        .andExpect(content().string(containsString("Next")));
    mvc.perform(get(url).param("page", "2"))
        .andExpect(content().string(containsString("Showing 26–26 of 26")));
  }

  @Test
  void filtersByArrival() throws Exception {
    Order arrived = purchases.buy(tier, 1, "Early Arrival", "early@example.com");
    purchases.buy(tier, 1, "Still Outside", "late@example.com");
    checkIn.checkIn(
        event.getId(), orders.tickets(arrived.getId()).get(0).getCode(), "door@example.com");

    mvc.perform(get(url).param("arrival", "IN"))
        .andExpect(content().string(containsString("Early Arrival")))
        .andExpect(content().string(not(containsString("Still Outside"))));
    mvc.perform(get(url).param("arrival", "OUT"))
        .andExpect(content().string(containsString("Still Outside")))
        .andExpect(content().string(not(containsString("Early Arrival"))));
  }

  @Test
  void emptyState() throws Exception {
    mvc.perform(get(url)).andExpect(content().string(containsString("No tickets sold yet.")));
  }

  @Test
  void searchesByNameEmailCodeAndReference() throws Exception {
    Order ana = purchases.buy(tier, 1, "Ana Popescu", "ana@example.com");
    purchases.buy(tier, 1, "Bogdan Stan", "bogdan@mail.test");
    String code = orders.tickets(ana.getId()).get(0).getCode();

    mvc.perform(get(url).param("q", "popes"))
        .andExpect(content().string(containsString("Ana Popescu")))
        .andExpect(content().string(not(containsString("Bogdan Stan"))));
    mvc.perform(get(url).param("q", "MAIL.TEST"))
        .andExpect(content().string(containsString("Bogdan Stan")));
    mvc.perform(get(url).param("q", code.toLowerCase()))
        .andExpect(content().string(containsString("Ana Popescu")));
    mvc.perform(get(url).param("q", ana.getReference()))
        .andExpect(content().string(containsString("Ana Popescu")));
  }

  @Test
  void wildcardsInTheQueryAreLiteral() throws Exception {
    purchases.buy(tier, 1, "Ana Popescu", "ana@example.com");

    mvc.perform(get(url).param("q", "%"))
        .andExpect(content().string(containsString("Nobody matches")));
  }
}

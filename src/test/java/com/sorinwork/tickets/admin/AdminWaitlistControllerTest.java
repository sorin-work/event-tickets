package com.sorinwork.tickets.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.stringContainsInOrder;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.waitlist.WaitlistForm;
import com.sorinwork.tickets.waitlist.WaitlistService;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminWaitlistControllerTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired WaitlistService waitlist;

  @Test
  void listsEntriesInTheOrderTheyJoined() throws Exception {
    Event event = data.event("Small Gig");
    TicketTier tier = data.tier(event, "Floor", "15", 1);
    event = events.publish(event.getId());
    purchases.buy(tier, 1, "Buyer", "buyer@example.com");
    waitlist.join(event, new WaitlistForm(tier.getId(), "Ana Pop", "ana@example.com", 1));
    clock.advance(Duration.ofMinutes(5));
    waitlist.join(event, new WaitlistForm(tier.getId(), "Bogdan Stan", "bogdan@example.com", 2));

    mvc.perform(get("/admin/events/" + event.getId() + "/waitlist"))
        .andExpect(status().isOk())
        .andExpect(content().string(stringContainsInOrder("Ana Pop", "Bogdan Stan")))
        .andExpect(content().string(containsString("Waiting")));
  }

  @Test
  void emptyState() throws Exception {
    Event event = data.event("Small Gig");

    mvc.perform(get("/admin/events/" + event.getId() + "/waitlist"))
        .andExpect(content().string(containsString("Nobody is on the waitlist.")));
  }
}

package com.sorinwork.tickets.waitlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.notify.OutboxRepository;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WaitlistLeaveTest extends IntegrationTest {

  @Autowired WaitlistService waitlist;
  @Autowired WaitlistRepository entries;
  @Autowired EventService events;
  @Autowired OrderAdminService orders;
  @Autowired OutboxRepository outbox;

  Event event;
  TicketTier tier;
  Order sold;
  WaitlistEntry entry;

  @BeforeEach
  void setUp() {
    event = data.event("Small Gig");
    tier = data.tier(event, "Floor", "15", 1);
    event = events.publish(event.getId());
    sold = purchases.buy(tier, 1, "Buyer", "buyer@example.com");
    entry = waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", "ana@example.com", 1));
  }

  @Test
  void joiningSendsALeaveLink() {
    assertThat(outbox.findByRecipientOrderByIdAsc("ana@example.com"))
        .singleElement()
        .satisfies(
            m -> assertThat(m.getBody()).contains("/waitlist/leave/" + entry.getLeaveToken()));
  }

  @Test
  void leavingTakesThePersonOffTheList() throws Exception {
    String url = "/waitlist/leave/" + entry.getLeaveToken();
    mvc.perform(get(url)).andExpect(content().string(containsString("Leave the waitlist")));

    mvc.perform(post(url).with(csrf()))
        .andExpect(content().string(containsString("no longer on the waitlist")));

    assertThat(entries.findById(entry.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.REMOVED);
    orders.cancel(sold.getId());
    assertThat(entries.findById(entry.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.REMOVED);
  }

  @Test
  void anOfferCannotBeUndoneFromTheLink() throws Exception {
    orders.cancel(sold.getId());

    mvc.perform(post("/waitlist/leave/" + entry.getLeaveToken()).with(csrf()))
        .andExpect(content().string(containsString("already been offered")));
    assertThat(entries.findById(entry.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.OFFERED);
  }

  @Test
  void unknownTokens() throws Exception {
    mvc.perform(get("/waitlist/leave/nope")).andExpect(status().isNotFound());
  }
}

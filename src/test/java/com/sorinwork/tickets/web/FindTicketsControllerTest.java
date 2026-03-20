package com.sorinwork.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.notify.OutboxMessage;
import com.sorinwork.tickets.notify.OutboxRepository;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class FindTicketsControllerTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired OutboxRepository outbox;

  @Test
  void emailsLinksForUpcomingOrders() throws Exception {
    Event event = data.event("Choir");
    TicketTier tier = data.tier(event, "Seat", "10", 20);
    events.publish(event.getId());
    Order first = purchases.buy(tier, 1, "Ana", "ana@example.com");
    Order second = purchases.buy(tier, 2, "Ana", "ana@example.com");

    mvc.perform(post("/tickets/find").with(csrf()).param("email", " ANA@example.com "))
        .andExpect(content().string(containsString("we have emailed you the links")))
        .andExpect(
            content().string(org.hamcrest.Matchers.not(containsString(first.getAccessToken()))));

    List<OutboxMessage> sent = outbox.findByRecipientOrderByIdAsc("ana@example.com");
    OutboxMessage links = sent.get(sent.size() - 1);
    assertThat(links.getSubject()).isEqualTo("Your tickets");
    assertThat(links.getBody()).contains(first.getAccessToken()).contains(second.getAccessToken());
  }

  @Test
  void unknownEmailsGetTheSameAnswerButNoEmail() throws Exception {
    mvc.perform(post("/tickets/find").with(csrf()).param("email", "nobody@example.com"))
        .andExpect(content().string(containsString("we have emailed you the links")));

    assertThat(outbox.findByRecipientOrderByIdAsc("nobody@example.com")).isEmpty();
  }

  @Test
  void asksForAnEmail() throws Exception {
    mvc.perform(post("/tickets/find").with(csrf()).param("email", ""))
        .andExpect(content().string(containsString("Enter the email address")));
  }
}

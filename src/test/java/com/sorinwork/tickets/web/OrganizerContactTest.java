package com.sorinwork.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.notify.OutboxRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.TestPropertySource;

@TestPropertySource(properties = "app.organizer-email=hello@venue.example")
class OrganizerContactTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired OutboxRepository outbox;

  @Test
  void pagesAndEmailsShowTheOrganizerAddress() throws Exception {
    mvc.perform(get("/")).andExpect(content().string(containsString("mailto:hello@venue.example")));

    Event event = data.event("Open Studio");
    TicketTier tier = data.tier(event, "Visitor", "0", 20);
    events.publish(event.getId());
    purchases.buy(tier, 1, "Ana", "ana@example.com");

    assertThat(outbox.findByRecipientOrderByIdAsc("ana@example.com").get(0).getBody())
        .contains("Questions? Write to hello@venue.example.");
  }
}

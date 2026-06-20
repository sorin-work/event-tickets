package com.sorinwork.tickets.api;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventApiControllerTest extends IntegrationTest {

  @Autowired EventService events;

  TicketTier tier;

  @BeforeEach
  void setUp() {
    Event event = data.event("Open Air Cinema");
    tier = data.tier(event, "Blanket spot", "7.50", 3);
    events.publish(event.getId());
    data.event("Draft Thing");
  }

  @Test
  void listsUpcomingPublishedEvents() throws Exception {
    mvc.perform(get("/api/v1/events"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].slug").value("open-air-cinema"))
        .andExpect(jsonPath("$[0].startsAt").value("2030-03-14T19:30:00"))
        .andExpect(jsonPath("$[0].timeZone").value("Europe/Bucharest"));
  }

  @Test
  void showsTiersWithAvailability() throws Exception {
    purchases.buy(tier, 2, "Ana", "ana@example.com");

    mvc.perform(get("/api/v1/events/open-air-cinema"))
        .andExpect(jsonPath("$.event.title").value("Open Air Cinema"))
        .andExpect(jsonPath("$.tiers[0].name").value("Blanket spot"))
        .andExpect(jsonPath("$.tiers[0].priceCents").value(750))
        .andExpect(jsonPath("$.tiers[0].currency").value("EUR"))
        .andExpect(jsonPath("$.tiers[0].remaining").value(1))
        .andExpect(jsonPath("$.tiers[0].soldOut").value(false))
        .andExpect(jsonPath("$.tiers[0].maxPerOrder").value(10))
        .andExpect(jsonPath("$.tiers[0].paused").value(false));
  }

  @Test
  void unknownEventIsAJson404() throws Exception {
    mvc.perform(get("/api/v1/events/draft-thing"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.error").value("Not found"));
  }
}

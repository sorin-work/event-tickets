package com.sorinwork.tickets.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class HomeControllerTest extends IntegrationTest {

  @Autowired EventService events;

  @Test
  void showsEmptyState() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No events on sale yet")));
  }

  @Test
  void searchesTitlesAndVenuesIgnoringAccents() throws Exception {
    Event cafe = data.event("Café Concert");
    data.tier(cafe, "Entry", "5", 30);
    events.publish(cafe.getId());
    Event other = data.event("Board Games");
    data.tier(other, "Entry", "5", 30);
    events.publish(other.getId());

    mvc.perform(get("/").param("q", "cafe"))
        .andExpect(content().string(containsString("Café Concert")))
        .andExpect(content().string(not(containsString("Board Games"))));
    mvc.perform(get("/").param("q", "attic games"))
        .andExpect(content().string(containsString("Board Games")));
    mvc.perform(get("/").param("q", "opera"))
        .andExpect(content().string(containsString("No upcoming events match")));
  }

  @Test
  void flagsSoldOutAndAlmostSoldOutEvents() throws Exception {
    Event full = data.event("Tiny Gig");
    var tier = data.tier(full, "Entry", "5", 2);
    events.publish(full.getId());
    purchases.buy(tier, 2, "Ana", "ana@example.com");
    Event almost = data.event("Small Gig");
    data.tier(almost, "Entry", "5", 4);
    events.publish(almost.getId());

    mvc.perform(get("/"))
        .andExpect(content().string(containsString("Sold out")))
        .andExpect(content().string(containsString("Only 4 left")));
  }

  @Test
  void listsOnlyPublishedEvents() throws Exception {
    Event published = data.event("Poetry Slam");
    data.tier(published, "Entry", "5", 30);
    events.publish(published.getId());
    data.event("Secret Draft");

    mvc.perform(get("/"))
        .andExpect(content().string(containsString("Poetry Slam")))
        .andExpect(content().string(containsString("Thu 14 Mar 2030, 19:30")))
        .andExpect(content().string(not(containsString("Secret Draft"))));
  }
}

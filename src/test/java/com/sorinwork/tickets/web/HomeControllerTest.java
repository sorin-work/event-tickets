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

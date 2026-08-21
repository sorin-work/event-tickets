package com.sorinwork.tickets.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PublicEventControllerTest extends IntegrationTest {

  @Autowired EventService events;

  @Test
  void showsPublishedEventWithTiers() throws Exception {
    Event event = data.event("Poetry Slam");
    data.tier(event, "Standing", "8.50", 30);
    data.tier(event, "Members", "0", 10);
    events.publish(event.getId());

    mvc.perform(get("/events/poetry-slam"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Standing")))
        .andExpect(content().string(containsString("€8.50")))
        .andExpect(content().string(containsString("Free")));
  }

  @Test
  void offersACalendarFile() throws Exception {
    Event event = data.event("Poetry Slam");
    data.tier(event, "Standing", "8.50", 30);
    events.publish(event.getId());

    mvc.perform(get("/events/poetry-slam/calendar.ics"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith("text/calendar"))
        .andExpect(content().string(containsString("SUMMARY:Poetry Slam")))
        .andExpect(
            content().string(containsString("URL:http://localhost:8080/events/poetry-slam")));
  }

  @Test
  void hasLinkPreviewTags() throws Exception {
    Event event = data.event("Poetry Slam");
    data.tier(event, "Standing", "8.50", 30);
    events.publish(event.getId());

    mvc.perform(get("/events/poetry-slam"))
        .andExpect(
            content()
                .string(containsString("<meta property=\"og:title\" content=\"Poetry Slam\">")))
        .andExpect(
            content()
                .string(
                    containsString(
                        "<meta property=\"og:url\" content=\"http://localhost:8080/events/poetry-slam\">")));
  }

  @Test
  void showsSalesWindows() throws Exception {
    Event event = data.event("Poetry Slam");
    var early = data.tier(event, "Early", "5", 30);
    early.setSalesEnd(java.time.LocalDateTime.of(2030, 1, 10, 23, 59));
    data.saveTier(early);
    var late = data.tier(event, "Late", "9", 30);
    late.setSalesStart(java.time.LocalDateTime.of(2030, 2, 1, 10, 0));
    data.saveTier(late);
    events.publish(event.getId());

    mvc.perform(get("/events/poetry-slam"))
        .andExpect(content().string(containsString("Sales have ended")))
        .andExpect(content().string(containsString("On sale from Fri 1 Feb 2030, 10:00")))
        .andExpect(
            content().string(org.hamcrest.Matchers.not(containsString("qty_" + late.getId()))));
  }

  @Test
  void accentedTitlesGetReadableAddresses() throws Exception {
    Event event = data.event("Seară de Poezie în Cafenea");
    data.tier(event, "Intrare", "10", 30);
    events.publish(event.getId());

    mvc.perform(get("/events/seara-de-poezie-in-cafenea")).andExpect(status().isOk());
  }

  @Test
  void draftsAreNotFound() throws Exception {
    data.event("Poetry Slam");

    mvc.perform(get("/events/poetry-slam").accept("text/html")).andExpect(status().isNotFound());
  }

  @Test
  void cancelledEventsShowANotice() throws Exception {
    Event event = data.event("Poetry Slam");
    events.cancel(event.getId());

    mvc.perform(get("/events/poetry-slam"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("has been cancelled")));
  }
}

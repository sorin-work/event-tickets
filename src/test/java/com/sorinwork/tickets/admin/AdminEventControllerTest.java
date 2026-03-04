package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminEventControllerTest extends IntegrationTest {

  @Autowired EventRepository events;
  @Autowired com.sorinwork.tickets.event.EventService eventService;

  @Test
  void showsEmptyState() throws Exception {
    mvc.perform(get("/admin/events"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("No events yet")));
  }

  @Test
  void createsEvent() throws Exception {
    mvc.perform(
            post("/admin/events")
                .with(csrf())
                .param("title", "Rust Meetup #4")
                .param("venueName", "Hub Space")
                .param("startsAt", "2030-05-02T19:00")
                .param("timeZone", "Europe/Bucharest")
                .param("currency", "RON")
                .param("capacity", "150"))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("/admin/events/*/edit"));

    Event event = events.findBySlug("rust-meetup-4").orElseThrow();
    assertThat(event.getStartsAt()).isEqualTo(LocalDateTime.of(2030, 5, 2, 19, 0));
    assertThat(event.getCurrency()).isEqualTo("RON");
    assertThat(event.getCapacity()).isEqualTo(150);
  }

  @Test
  void rejectsEndBeforeStart() throws Exception {
    mvc.perform(
            post("/admin/events")
                .with(csrf())
                .param("title", "Backwards")
                .param("venueName", "Hub Space")
                .param("startsAt", "2030-05-02T19:00")
                .param("endsAt", "2030-05-02T18:00")
                .param("timeZone", "Europe/Bucharest")
                .param("currency", "EUR"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "endAfterStart"));

    assertThat(events.count()).isZero();
  }

  @Test
  void rejectsUnknownTimeZoneAndCurrency() throws Exception {
    mvc.perform(
            post("/admin/events")
                .with(csrf())
                .param("title", "Somewhere")
                .param("venueName", "Hub Space")
                .param("startsAt", "2030-05-02T19:00")
                .param("timeZone", "Mars/Olympus")
                .param("currency", "euro"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "timeZoneValid", "currency"));
  }

  @Test
  void updatesEvent() throws Exception {
    mvc.perform(
        post("/admin/events")
            .with(csrf())
            .param("title", "Quiz Night")
            .param("venueName", "The Owl")
            .param("startsAt", "2030-05-02T19:00")
            .param("timeZone", "Europe/Bucharest")
            .param("currency", "EUR"));
    Event event = events.findBySlug("quiz-night").orElseThrow();

    mvc.perform(
            post("/admin/events/" + event.getId())
                .with(csrf())
                .param("title", "Quiz Night")
                .param("venueName", "The Owl, upstairs")
                .param("startsAt", "2030-05-02T20:00")
                .param("timeZone", "Europe/Bucharest")
                .param("currency", "EUR"))
        .andExpect(status().is3xxRedirection());

    assertThat(events.findById(event.getId()).orElseThrow().getVenueName())
        .isEqualTo("The Owl, upstairs");
  }

  @Test
  void listShowsTicketsSold() throws Exception {
    Event event = data.event("Lantern Walk");
    var tier = data.tier(event, "Walker", "0", 30);
    eventService.publish(event.getId());
    purchases.buy(tier, 3, "Ana", "ana@example.com");

    mvc.perform(get("/admin/events"))
        .andExpect(content().string(containsString("<td class=\"num\">3</td>")));
  }

  @Test
  void unknownEventIs404() throws Exception {
    mvc.perform(get("/admin/events/12345/edit")).andExpect(status().isNotFound());
  }
}

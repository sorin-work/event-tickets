package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminDeleteTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired EventRepository eventRepository;
  @Autowired TicketTierRepository tiers;

  @Test
  void deletesADraftWithItsTiers() throws Exception {
    Event draft = data.event("Oops");
    data.tier(draft, "Entry", "5", 10);

    mvc.perform(post("/admin/events/" + draft.getId() + "/delete").with(csrf()))
        .andExpect(redirectedUrl("/admin/events"));

    assertThat(eventRepository.existsById(draft.getId())).isFalse();
    assertThat(tiers.count()).isZero();
  }

  @Test
  void publishedEventsCannotBeDeleted() throws Exception {
    Event event = data.event("Live");
    data.tier(event, "Entry", "5", 10);
    events.publish(event.getId());

    mvc.perform(post("/admin/events/" + event.getId() + "/delete").with(csrf()))
        .andExpect(
            flash().attribute("error", "Only draft events can be deleted. Cancel it instead."));
    assertThat(eventRepository.existsById(event.getId())).isTrue();
  }

  @Test
  void draftsThatSoldTicketsBeforeCannotBeDeleted() throws Exception {
    Event event = data.event("Back To Draft");
    TicketTier tier = data.tier(event, "Entry", "5", 10);
    events.publish(event.getId());
    purchases.buy(tier, 1, "Ana", "ana@example.com");
    events.unpublish(event.getId());

    mvc.perform(post("/admin/events/" + event.getId() + "/delete").with(csrf()))
        .andExpect(flash().attribute("error", "This event has orders and cannot be deleted."));
  }

  @Test
  void tiersWithOrdersCannotBeRemoved() throws Exception {
    Event event = data.event("Live");
    TicketTier tier = data.tier(event, "Entry", "5", 10);
    events.publish(event.getId());
    purchases.buy(tier, 1, "Ana", "ana@example.com");

    mvc.perform(
            post("/admin/events/" + event.getId() + "/tiers/" + tier.getId() + "/delete")
                .with(csrf()))
        .andExpect(
            flash()
                .attribute(
                    "error", "Entry has orders and cannot be removed. Pause its sales instead."));
    assertThat(tiers.existsById(tier.getId())).isTrue();
  }

  @Test
  void unsoldTiersCanBeRemoved() throws Exception {
    Event event = data.event("Live");
    TicketTier tier = data.tier(event, "Entry", "5", 10);

    mvc.perform(
        post("/admin/events/" + event.getId() + "/tiers/" + tier.getId() + "/delete").with(csrf()));

    assertThat(tiers.existsById(tier.getId())).isFalse();
  }
}

package com.sorinwork.tickets.waitlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WaitlistJoinTest extends IntegrationTest {

  @Autowired WaitlistService waitlist;
  @Autowired WaitlistRepository entries;
  @Autowired EventService events;

  Event event;
  TicketTier tier;

  @BeforeEach
  void setUp() {
    event = data.event("Small Gig");
    tier = data.tier(event, "Floor", "15", 2);
    event = events.publish(event.getId());
  }

  private void sellOut() {
    purchases.buy(tier, 2, "Early Bird", "early@example.com");
  }

  @Test
  void joinsWhenSoldOut() {
    sellOut();

    WaitlistEntry entry =
        waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", " Ana@Example.com ", 2));

    assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.WAITING);
    assertThat(entry.getEmail()).isEqualTo("ana@example.com");
    assertThat(waitlist.waitingCount(tier.getId())).isEqualTo(1);
  }

  @Test
  void cannotJoinWhileTicketsAreOnSale() {
    assertThatThrownBy(
            () -> waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", "a@b.co", 1)))
        .hasMessageContaining("still on sale");
  }

  @Test
  void cannotJoinTwice() {
    sellOut();
    waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", "ana@example.com", 1));

    assertThatThrownBy(
            () -> waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", "ANA@example.com", 1)))
        .hasMessageContaining("already on the waitlist");
  }

  @Test
  void respectsMaxPerOrder() {
    sellOut();

    assertThatThrownBy(
            () -> waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", "a@b.co", 11)))
        .hasMessageContaining("at most 10");
  }

  @Test
  void eventPageLinksToTheWaitlistForSoldOutTiers() throws Exception {
    sellOut();

    mvc.perform(get("/events/small-gig"))
        .andExpect(content().string(containsString("join the waitlist")));
    mvc.perform(get("/events/small-gig/waitlist").param("tier", tier.getId().toString()))
        .andExpect(content().string(containsString("Floor</strong> is sold out")));
  }

  @Test
  void tellsPeopleTheirPlaceInTheQueue() throws Exception {
    sellOut();
    waitlist.join(event, new WaitlistForm(tier.getId(), "First", "first@example.com", 1));

    mvc.perform(
            post("/events/small-gig/waitlist")
                .with(csrf())
                .param("tierId", tier.getId().toString())
                .param("name", "Ana Pop")
                .param("email", "ana@example.com")
                .param("quantity", "1"))
        .andExpect(
            flash().attribute("notice", org.hamcrest.Matchers.startsWith("You are number 2")));
  }

  @Test
  void eventPageShowsHowManyAreWaiting() throws Exception {
    sellOut();
    waitlist.join(event, new WaitlistForm(tier.getId(), "Ana", "ana@example.com", 1));
    waitlist.join(event, new WaitlistForm(tier.getId(), "Bob", "bob@example.com", 2));

    mvc.perform(get("/events/small-gig"))
        .andExpect(content().string(containsString("(2 people waiting)")));
  }

  @Test
  void joinsThroughTheForm() throws Exception {
    sellOut();

    mvc.perform(
            post("/events/small-gig/waitlist")
                .with(csrf())
                .param("tierId", tier.getId().toString())
                .param("name", "Ana Pop")
                .param("email", "ana@example.com")
                .param("quantity", "1"))
        .andExpect(redirectedUrl("/events/small-gig"))
        .andExpect(flash().attributeExists("notice"));

    assertThat(entries.count()).isEqualTo(1);
  }
}

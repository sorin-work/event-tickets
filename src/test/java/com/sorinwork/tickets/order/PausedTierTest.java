package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TierForm;
import com.sorinwork.tickets.event.TierService;
import com.sorinwork.tickets.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PausedTierTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired TierService tiers;
  @Autowired CheckoutService checkout;

  Event event;
  TicketTier tier;

  @BeforeEach
  void setUp() {
    event = data.event("Late Show");
    tier = data.tier(event, "Balcony", "25", 40);
    events.publish(event.getId());
    TierForm form = TierForm.from(tier);
    form.setPaused(true);
    tiers.update(event.getId(), tier.getId(), form);
  }

  @Test
  void pausedTiersCannotBeBought() {
    assertThatThrownBy(() -> checkout.start(event.getId(), Map.of(tier.getId(), 1)))
        .hasMessage("Balcony is not on sale right now.");
  }

  @Test
  void eventPageShowsThePause() throws Exception {
    mvc.perform(get("/events/late-show"))
        .andExpect(content().string(containsString("Not on sale right now")))
        .andExpect(content().string(not(containsString("name=\"qty_" + tier.getId() + "\""))));
  }

  @Test
  void resumingSalesWorks() {
    TierForm form = TierForm.from(tiers.get(event.getId(), tier.getId()));
    form.setPaused(false);
    tiers.update(event.getId(), tier.getId(), form);

    checkout.start(event.getId(), Map.of(tier.getId(), 1));
  }
}

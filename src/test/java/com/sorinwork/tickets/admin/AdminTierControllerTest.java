package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminTierControllerTest extends IntegrationTest {

  @Autowired TicketTierRepository tiers;
  @Autowired com.sorinwork.tickets.event.EventService events;

  Event event;

  @BeforeEach
  void setUp() {
    event = data.event("Indie Night");
  }

  @Test
  void addsTierAndShowsItOnTheEventPage() throws Exception {
    mvc.perform(
            post("/admin/events/" + event.getId() + "/tiers")
                .with(csrf())
                .param("name", "Early bird")
                .param("price", "12,50")
                .param("quantity", "40")
                .param("maxPerOrder", "4"))
        .andExpect(status().is3xxRedirection());

    List<TicketTier> saved = tiers.findByEventIdOrderByPositionAscIdAsc(event.getId());
    assertThat(saved).singleElement().satisfies(t -> assertThat(t.getPriceCents()).isEqualTo(1250));

    mvc.perform(get("/admin/events/" + event.getId() + "/edit"))
        .andExpect(content().string(containsString("Early bird")))
        .andExpect(content().string(containsString("€12.50")));
  }

  @Test
  void movesTiersUpAndDown() throws Exception {
    TicketTier first = data.tier(event, "First", "10", 10);
    TicketTier second = data.tier(event, "Second", "10", 10);
    TicketTier third = data.tier(event, "Third", "10", 10);

    mvc.perform(
            post("/admin/events/" + event.getId() + "/tiers/" + third.getId() + "/move")
                .with(csrf())
                .param("direction", "-1"))
        .andExpect(status().is3xxRedirection());
    mvc.perform(
        post("/admin/events/" + event.getId() + "/tiers/" + first.getId() + "/move")
            .with(csrf())
            .param("direction", "-1"));

    assertThat(tiers.findByEventIdOrderByPositionAscIdAsc(event.getId()))
        .extracting(TicketTier::getName)
        .containsExactly("First", "Third", "Second");
    assertThat(second.getId()).isNotNull();
  }

  @Test
  void rejectsInvalidPrice() throws Exception {
    mvc.perform(
            post("/admin/events/" + event.getId() + "/tiers")
                .with(csrf())
                .param("name", "Early bird")
                .param("price", "12.505")
                .param("quantity", "40")
                .param("maxPerOrder", "4"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "priceValid"));
  }

  @Test
  void rejectsSalesEndBeforeStart() throws Exception {
    mvc.perform(
            post("/admin/events/" + event.getId() + "/tiers")
                .with(csrf())
                .param("name", "Early bird")
                .param("price", "10")
                .param("quantity", "40")
                .param("maxPerOrder", "4")
                .param("salesStart", "2030-01-10T10:00")
                .param("salesEnd", "2030-01-09T10:00"))
        .andExpect(model().attributeHasFieldErrors("form", "salesWindowValid"));
  }

  @Test
  void tierOfAnotherEventIsNotFound() throws Exception {
    Event other = data.event("Other");
    mvc.perform(
        post("/admin/events/" + other.getId() + "/tiers")
            .with(csrf())
            .param("name", "VIP")
            .param("price", "50")
            .param("quantity", "5")
            .param("maxPerOrder", "2"));
    long tierId = tiers.findByEventIdOrderByPositionAscIdAsc(other.getId()).get(0).getId();

    mvc.perform(get("/admin/events/" + event.getId() + "/tiers/" + tierId + "/edit"))
        .andExpect(status().isNotFound());
  }

  @Test
  void aQuantityBelowWhatIsSoldIsShownOnTheForm() throws Exception {
    TicketTier tier = data.tier(event, "Floor", "10", 5);
    events.publish(event.getId());
    purchases.buy(tier, 3, "Ana", "ana@example.com");

    mvc.perform(
            post("/admin/events/" + event.getId() + "/tiers/" + tier.getId())
                .with(csrf())
                .param("name", "Floor")
                .param("price", "10")
                .param("quantity", "2")
                .param("maxPerOrder", "10"))
        .andExpect(status().isOk())
        .andExpect(model().attributeHasFieldErrors("form", "quantity"))
        .andExpect(content().string(containsString("That is fewer than are taken: 3 sold.")));

    assertThat(tiers.findById(tier.getId()).orElseThrow().getQuantity()).isEqualTo(5);
  }
}

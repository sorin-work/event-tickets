package com.sorinwork.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckoutControllerTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired OrderRepository orders;

  Event event;
  TicketTier tier;

  @BeforeEach
  void setUp() {
    event = data.event("Film Club");
    tier = data.tier(event, "Seat", "6", 30);
    events.publish(event.getId());
  }

  @Test
  void eventPageOffersTicketSelection() throws Exception {
    mvc.perform(get("/events/film-club"))
        .andExpect(content().string(containsString("name=\"qty_" + tier.getId() + "\"")));
  }

  @Test
  void startsCheckoutAndShowsTheOrder() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "2"))
        .andExpect(status().is3xxRedirection());

    assertThat(orders.count()).isEqualTo(1);
    Order order = orders.findAll().get(0);

    mvc.perform(get("/checkout/" + order.getAccessToken()))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("2 × Seat")))
        .andExpect(content().string(containsString("€12.00")));
  }

  @Test
  void returnsToEventPageWithAnError() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "0"))
        .andExpect(redirectedUrl("/events/film-club"))
        .andExpect(flash().attribute("error", "Choose at least one ticket."));
  }

  @Test
  void showsExpiredReservation() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "1"));
    Order order = orders.findAll().get(0);
    clock.advance(Duration.ofMinutes(15));

    mvc.perform(get("/checkout/" + order.getAccessToken()))
        .andExpect(content().string(containsString("reservation has expired")));
  }

  @Test
  void unknownTokenIsNotFound() throws Exception {
    mvc.perform(get("/checkout/nope")).andExpect(status().isNotFound());
  }

  @Test
  void soldOutTiersCannotBeSelected() throws Exception {
    TicketTier tiny = data.tier(event, "Box", "50", 1);
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tiny.getId(), "1"));

    mvc.perform(get("/events/film-club"))
        .andExpect(content().string(containsString("Sold out")))
        .andExpect(
            content().string(org.hamcrest.Matchers.not(containsString("qty_" + tiny.getId()))));
  }
}

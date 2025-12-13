package com.sorinwork.tickets.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.promo.DiscountType;
import com.sorinwork.tickets.promo.PromoCode;
import com.sorinwork.tickets.promo.PromoCodeRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PromoCheckoutControllerTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired CheckoutService checkout;
  @Autowired PromoCodeRepository promos;

  String token;

  @BeforeEach
  void setUp() {
    Event event = data.event("Synth Workshop");
    TicketTier tier = data.tier(event, "Seat", "40", 30);
    events.publish(event.getId());
    promos.save(new PromoCode(event, "FRIENDS", DiscountType.PERCENT, 25, clock.instant()));
    token = checkout.start(event.getId(), Map.of(tier.getId(), 1)).getAccessToken();
  }

  @Test
  void htmxRequestGetsTheUpdatedSummary() throws Exception {
    mvc.perform(
            post("/checkout/" + token + "/promo")
                .with(csrf())
                .header("HX-Request", "true")
                .param("code", "friends"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("id=\"order-summary\"")))
        .andExpect(content().string(containsString("-€10.00")))
        .andExpect(content().string(containsString("€30.00")))
        .andExpect(content().string(not(containsString("<html"))));
  }

  @Test
  void invalidCodeShowsAnError() throws Exception {
    mvc.perform(
            post("/checkout/" + token + "/promo")
                .with(csrf())
                .header("HX-Request", "true")
                .param("code", "NOPE"))
        .andExpect(content().string(containsString("That promo code is not valid.")));
  }

  @Test
  void worksWithoutJavascript() throws Exception {
    mvc.perform(post("/checkout/" + token + "/promo").with(csrf()).param("code", "FRIENDS"))
        .andExpect(redirectedUrl("/checkout/" + token));

    mvc.perform(get("/checkout/" + token)).andExpect(content().string(containsString("FRIENDS")));
  }

  @Test
  void removesCode() throws Exception {
    mvc.perform(post("/checkout/" + token + "/promo").with(csrf()).param("code", "FRIENDS"));

    mvc.perform(
            post("/checkout/" + token + "/promo/remove").with(csrf()).header("HX-Request", "true"))
        .andExpect(content().string(not(containsString("-€10.00"))))
        .andExpect(content().string(containsString("€40.00")));
  }

  @Test
  void pagesLoadHtmx() throws Exception {
    mvc.perform(get("/checkout/" + token))
        .andExpect(content().string(containsString("/dist/htmx.min.js")))
        .andExpect(content().string(containsString("name=\"_csrf\"")));
    mvc.perform(get("/webjars/htmx.org/dist/htmx.min.js")).andExpect(status().isOk());
  }
}

package com.sorinwork.tickets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.HoldExpiryService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderRepository;
import com.sorinwork.tickets.promo.DiscountType;
import com.sorinwork.tickets.promo.PromoCode;
import com.sorinwork.tickets.promo.PromoCodeRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.TicketRepository;
import com.sorinwork.tickets.waitlist.WaitlistRepository;
import com.sorinwork.tickets.waitlist.WaitlistStatus;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** One evening from the first ticket sold to the last person through the door. */
class JourneyTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired PromoCodeRepository promos;
  @Autowired OrderRepository orders;
  @Autowired TicketRepository tickets;
  @Autowired WaitlistRepository waitlist;
  @Autowired HoldExpiryService expiry;

  @Test
  void sellPromoWaitlistAndDoor() throws Exception {
    Event event = data.event("Rooftop Cinema");
    TicketTier seat = data.tier(event, "Deck chair", "12", 2);
    events.publish(event.getId());
    promos.save(new PromoCode(event, "FRIENDS", DiscountType.PERCENT, 50, clock.instant()));

    // Ana buys both seats with a promo code.
    mvc.perform(
        post("/events/rooftop-cinema/checkout").with(csrf()).param("qty_" + seat.getId(), "2"));
    String ana = orders.findAll().get(0).getAccessToken();
    mvc.perform(post("/checkout/" + ana + "/promo").with(csrf()).param("code", "friends"));
    mvc.perform(
        post("/checkout/" + ana)
            .with(csrf())
            .param("name", "Ana Pop")
            .param("email", "ana@example.com"));
    Order paid = orders.findByAccessToken(ana).orElseThrow();
    assertThat(paid.getTotalCents()).isEqualTo(1200);

    // Bob finds it sold out and joins the waitlist.
    mvc.perform(get("/events/rooftop-cinema"))
        .andExpect(content().string(containsString("join the waitlist")));
    mvc.perform(
        post("/events/rooftop-cinema/waitlist")
            .with(csrf())
            .param("tierId", seat.getId().toString())
            .param("name", "Bob Stan")
            .param("email", "bob@example.com")
            .param("quantity", "1"));

    // Ana cancels one evening later via the organiser; Bob gets the offer and buys.
    mvc.perform(post("/admin/orders/" + paid.getId() + "/cancel").with(csrf()).with(user("admin")));
    var entry = waitlist.findAll().get(0);
    assertThat(entry.getStatus()).isEqualTo(WaitlistStatus.OFFERED);
    String bob =
        waitlist.findWithOfferById(entry.getId()).orElseThrow().getOfferOrder().getAccessToken();
    clock.advance(Duration.ofHours(2));
    mvc.perform(
        post("/checkout/" + bob)
            .with(csrf())
            .param("name", "Bob Stan")
            .param("email", "bob@example.com"));
    expiry.expireHolds();
    assertThat(waitlist.findAll().get(0).getStatus()).isEqualTo(WaitlistStatus.CLAIMED);

    // At the door Bob's ticket works once, Ana's cancelled ones do not.
    Order bobsOrder = orders.findByAccessToken(bob).orElseThrow();
    String bobCode = tickets.findByOrderIdOrderById(bobsOrder.getId()).get(0).getCode();
    String anaCode = tickets.findByOrderIdOrderById(paid.getId()).get(0).getCode();
    String door = "/admin/events/" + event.getId() + "/check-in";
    mvc.perform(post(door).with(csrf()).with(user("door")).param("code", bobCode))
        .andExpect(content().string(containsString("Welcome in")));
    mvc.perform(post(door).with(csrf()).with(user("door")).param("code", bobCode))
        .andExpect(content().string(containsString("already checked in")));
    mvc.perform(post(door).with(csrf()).with(user("door")).param("code", anaCode))
        .andExpect(content().string(containsString("cancelled")));
  }
}

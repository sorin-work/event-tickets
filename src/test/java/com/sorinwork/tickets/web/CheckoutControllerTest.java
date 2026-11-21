package com.sorinwork.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderRepository;
import com.sorinwork.tickets.order.OrderStatus;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.TicketRepository;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckoutControllerTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired OrderRepository orders;
  @Autowired com.sorinwork.tickets.order.CheckoutService checkout;
  @Autowired TicketRepository tickets;

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
  void checkoutPageCountsDownToTheEndOfTheHold() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "1"));
    String token = orders.findAll().get(0).getAccessToken();

    mvc.perform(get("/checkout/" + token))
        .andExpect(content().string(containsString("data-countdown=\"2030-01-15T10:10:00Z\"")))
        .andExpect(content().string(containsString("(until <span>12:10</span>)")))
        .andExpect(content().string(containsString("Pay €6.00")))
        .andExpect(
            content()
                .string(containsString("<title>Checkout · Film Club · Event Tickets</title>")));
  }

  @Test
  void freeOrdersSayGetTickets() throws Exception {
    TicketTier free = data.tier(event, "Guest list", "0", 10);
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + free.getId(), "1"));
    String token = orders.findAll().get(0).getAccessToken();

    mvc.perform(get("/checkout/" + token))
        .andExpect(content().string(containsString("Get tickets")));
  }

  @Test
  void returnsToEventPageWithAnError() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "0"))
        .andExpect(redirectedUrl("/events/film-club"))
        .andExpect(flash().attribute("error", "Choose at least one ticket."));
  }

  @Test
  void negativeQuantitiesFromAHandMadeFormAreRejected() throws Exception {
    TicketTier other = data.tier(event, "Balcony", "20", 10);

    mvc.perform(
            post("/events/film-club/checkout")
                .with(csrf())
                .param("qty_" + tier.getId(), "-5")
                .param("qty_" + other.getId(), "1"))
        .andExpect(redirectedUrl("/events/film-club"))
        .andExpect(flash().attribute("error", "Ticket quantities cannot be negative."));

    assertThat(orders.count()).isZero();
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

  @Test
  void completesCheckoutAndShowsTickets() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "2"));
    String token = orders.findAll().get(0).getAccessToken();

    mvc.perform(
            post("/checkout/" + token)
                .with(csrf())
                .param("name", "Ioana Rus")
                .param("email", "ioana@example.com"))
        .andExpect(redirectedUrl("/orders/" + token));

    mvc.perform(get("/orders/" + token))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Your order is confirmed")))
        .andExpect(content().string(containsString("Ioana Rus")));
    mvc.perform(get("/checkout/" + token)).andExpect(redirectedUrl("/orders/" + token));
  }

  @Test
  void eachTicketCanCarryItsOwnName() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "3"));
    Order order = orders.findAll().get(0);
    String token = order.getAccessToken();
    mvc.perform(get("/checkout/" + token)).andExpect(content().string(containsString("Seat 3")));

    mvc.perform(
        post("/checkout/" + token)
            .with(csrf())
            .param("name", "Ioana Rus")
            .param("email", "ioana@example.com")
            .param("attendeeNames[0]", "")
            .param("attendeeNames[1]", " Matei Rus ")
            .param("attendeeNames[2]", "Sara Rus"));

    assertThat(tickets.findByOrderIdOrderById(order.getId()))
        .extracting(com.sorinwork.tickets.ticket.Ticket::getAttendeeName)
        .containsExactly("Ioana Rus", "Matei Rus", "Sara Rus");
  }

  @Test
  void validatesBuyerDetails() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "1"));
    String token = orders.findAll().get(0).getAccessToken();

    mvc.perform(post("/checkout/" + token).with(csrf()).param("name", "").param("email", "nope"))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.model()
                .attributeHasFieldErrors("form", "name", "email"));
  }

  @Test
  void unpaidOrderPageGoesBackToCheckout() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "1"));
    String token = orders.findAll().get(0).getAccessToken();

    mvc.perform(get("/orders/" + token)).andExpect(redirectedUrl("/checkout/" + token));
  }

  @Test
  void submittingThePaymentFormTwiceKeepsOneSetOfTickets() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "2"));
    Order order = orders.findAll().get(0);
    String token = order.getAccessToken();

    for (int i = 0; i < 2; i++) {
      mvc.perform(
              post("/checkout/" + token)
                  .with(csrf())
                  .param("name", "Ioana Rus")
                  .param("email", "ioana@example.com"))
          .andExpect(redirectedUrl("/orders/" + token));
    }

    assertThat(tickets.findByOrderIdOrderById(order.getId())).hasSize(2);
  }

  private String holdCookie() {
    return CheckoutController.HOLD_COOKIE_PREFIX + event.getId();
  }

  @Test
  void startingACheckoutRemembersItInACookie() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "1"))
        .andExpect(
            header()
                .string(
                    "Set-Cookie",
                    containsString(holdCookie() + "=" + orders.findAll().get(0).getAccessToken())))
        .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
        .andExpect(header().string("Set-Cookie", containsString("SameSite=Lax")));
  }

  @Test
  void startingAgainReplacesTheBrowsersOpenCheckout() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "3"));
    Order first = orders.findAll().get(0);

    mvc.perform(
        post("/events/film-club/checkout")
            .with(csrf())
            .cookie(new Cookie(holdCookie(), first.getAccessToken()))
            .param("qty_" + tier.getId(), "2"));

    assertThat(orders.findById(first.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.EXPIRED);
    assertThat(orders.findAll()).filteredOn(o -> o.getStatus() == OrderStatus.PENDING).hasSize(1);
  }

  @Test
  void checkoutPageOffersToGiveTheTicketsBack() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "1"));
    String token = orders.findAll().get(0).getAccessToken();

    mvc.perform(get("/checkout/" + token))
        .andExpect(content().string(containsString("/checkout/" + token + "/release")))
        .andExpect(content().string(containsString("Cancel and give the tickets back")));
  }

  @Test
  void givingTheTicketsBackReturnsToTheEventPage() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "1"));
    Order order = orders.findAll().get(0);

    mvc.perform(post("/checkout/" + order.getAccessToken() + "/release").with(csrf()))
        .andExpect(redirectedUrl("/events/film-club"))
        .andExpect(flash().attribute("notice", containsString("given back")))
        .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));

    assertThat(orders.findById(order.getId()).orElseThrow().getStatus())
        .isEqualTo(OrderStatus.EXPIRED);
  }

  @Test
  void aPaidOrderCannotBeGivenBack() throws Exception {
    Order paid = purchases.buy(tier, 1, "Ana", "ana@example.com");

    mvc.perform(post("/checkout/" + paid.getAccessToken() + "/release").with(csrf()))
        .andExpect(redirectedUrl("/checkout/" + paid.getAccessToken()));

    assertThat(orders.findById(paid.getId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAID);
  }

  @Test
  void aVisitorHoldingSeatsIsToldToFinishFirst() throws Exception {
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "10"));

    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "5"))
        .andExpect(redirectedUrl("/events/film-club"))
        .andExpect(flash().attribute("error", containsString("You already have 10 tickets")));

    mvc.perform(
            post("/events/film-club/checkout")
                .with(csrf())
                .with(
                    request -> {
                      request.setRemoteAddr("198.51.100.20");
                      return request;
                    })
                .param("qty_" + tier.getId(), "5"))
        .andExpect(status().is3xxRedirection())
        .andExpect(flash().attributeCount(0));
  }

  @Test
  void tooManyStartsInAMinuteAreRefused() throws Exception {
    // Twenty starts from one browser, each replacing the one before, so the seat cap never applies.
    String previous = "none";
    for (int i = 0; i < 20; i++) {
      mvc.perform(
          post("/events/film-club/checkout")
              .with(csrf())
              .cookie(new Cookie(holdCookie(), previous))
              .param("qty_" + tier.getId(), "1"));
      previous =
          orders.findAll().stream()
              .filter(o -> o.getStatus() == OrderStatus.PENDING)
              .findFirst()
              .orElseThrow()
              .getAccessToken();
    }

    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "1"))
        .andExpect(flash().attribute("error", containsString("Too many attempts")));

    clock.advance(java.time.Duration.ofMinutes(1));
    mvc.perform(post("/events/film-club/checkout").with(csrf()).param("qty_" + tier.getId(), "1"))
        .andExpect(flash().attributeCount(0));
  }

  @Test
  void theStartLimitIsPerEvent() throws Exception {
    Event other = data.event("Quiz Night");
    TicketTier quiz = data.tier(other, "Team seat", "5", 100);
    events.publish(other.getId());
    String previous = "none";
    for (int i = 0; i < 20; i++) {
      mvc.perform(
          post("/events/film-club/checkout")
              .with(csrf())
              .cookie(new Cookie(holdCookie(), previous))
              .param("qty_" + tier.getId(), "1"));
      previous =
          orders.findAll().stream()
              .filter(o -> o.getStatus() == OrderStatus.PENDING)
              .findFirst()
              .orElseThrow()
              .getAccessToken();
    }

    mvc.perform(post("/events/quiz-night/checkout").with(csrf()).param("qty_" + quiz.getId(), "1"))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern(
                "/checkout/*"))
        .andExpect(flash().attributeCount(0));
  }
}

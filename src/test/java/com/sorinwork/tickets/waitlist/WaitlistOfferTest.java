package com.sorinwork.tickets.waitlist;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.notify.OutboxRepository;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.order.OrderStatus;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class WaitlistOfferTest extends IntegrationTest {

  @Autowired WaitlistService waitlist;
  @Autowired WaitlistRepository entries;
  @Autowired EventService events;
  @Autowired OrderAdminService orders;
  @Autowired CheckoutService checkout;
  @Autowired OutboxRepository outbox;

  Event event;
  TicketTier tier;
  Order first;
  Order second;

  @BeforeEach
  void setUp() {
    event = data.event("Small Gig");
    tier = data.tier(event, "Floor", "15", 3);
    event = events.publish(event.getId());
    first = purchases.buy(tier, 1, "First", "first@example.com");
    second = purchases.buy(tier, 2, "Second", "second@example.com");
  }

  private WaitlistEntry join(String name, int quantity) {
    return waitlist.join(
        event, new WaitlistForm(tier.getId(), name, name.toLowerCase() + "@example.com", quantity));
  }

  private String offerToken(WaitlistEntry entry) {
    return entries.findWithOfferById(entry.getId()).orElseThrow().getOfferOrder().getAccessToken();
  }

  @Test
  void releasedTicketsAreHeldForTheWaitingPerson() {
    WaitlistEntry entry = join("Ana", 1);
    orders.cancel(first.getId());

    WaitlistEntry offered = entries.findById(entry.getId()).orElseThrow();
    assertThat(offered.getStatus()).isEqualTo(WaitlistStatus.OFFERED);
    assertThat(offered.getOfferedAt()).isEqualTo(clock.instant());
    Order hold = checkout.getByToken(offerToken(entry));
    assertThat(hold.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(hold.getHoldExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofHours(24)));
    assertThat(hold.getBuyerEmail()).isEqualTo("ana@example.com");
  }

  @Test
  void heldTicketsAreNotSoldToTheNextVisitor() {
    join("Ana", 1);
    orders.cancel(first.getId());

    assertThatThrownBy(() -> checkout.start(event.getId(), Map.of(tier.getId(), 1)))
        .hasMessage("Floor is sold out.");
  }

  @Test
  void releasedTicketsGoToWhoeverJoinedFirst() {
    WaitlistEntry ana = join("Ana", 1);
    clock.advance(Duration.ofMinutes(30));
    WaitlistEntry bogdan = join("Bogdan", 1);
    clock.advance(Duration.ofMinutes(30));
    WaitlistEntry cara = join("Cara", 1);

    orders.cancel(first.getId());

    assertThat(entries.findById(ana.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.OFFERED);
    assertThat(entries.findById(bogdan.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.WAITING);
    assertThat(entries.findById(cara.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.WAITING);
  }

  @Test
  void theFirstInLineKeepsTheirPlaceWhenTheyWantMore() {
    WaitlistEntry ana = join("Ana", 2);
    clock.advance(Duration.ofMinutes(5));
    WaitlistEntry bogdan = join("Bogdan", 1);

    orders.cancel(first.getId());

    assertThat(entries.findById(ana.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.WAITING);
    assertThat(entries.findById(bogdan.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.WAITING);
  }

  @Test
  void personWantingMoreThanReleasedKeepsWaiting() {
    WaitlistEntry entry = join("Ana", 2);
    orders.cancel(first.getId());

    assertThat(entries.findById(entry.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.WAITING);
  }

  @Test
  void enoughReleasedTicketsServeEveryone() {
    join("Ana", 1);
    join("Bogdan", 2);
    orders.cancel(first.getId());
    orders.cancel(second.getId());

    assertThat(waitlist.waitingCount(tier.getId())).isZero();
  }

  @Test
  void offerEmailLinksToCheckout() {
    WaitlistEntry entry = join("Ana", 1);
    orders.cancel(first.getId());

    String token = offerToken(entry);
    assertThat(outbox.findByRecipientOrderByIdAsc("ana@example.com"))
        .filteredOn(m -> m.getSubject().startsWith("Tickets available"))
        .singleElement()
        .satisfies(
            m -> {
              assertThat(m.getSubject()).isEqualTo("Tickets available: Small Gig");
              assertThat(m.getBody()).contains("/checkout/" + token).contains("1 × Floor");
            });
  }

  @Test
  void cancellingAnOrderOffersItsSeatsRightAway() {
    join("Ana", 1);

    orders.cancel(first.getId());

    assertThat(waitlist.waitingCount(tier.getId())).isZero();
    assertThat(waitlist.offerReleasedTickets(event.getId())).isZero();
  }

  @Test
  void anOpenOfferCanBeSentAgain() {
    WaitlistEntry entry = join("Ana", 1);
    orders.cancel(first.getId());

    waitlist.resendOffer(event.getId(), entry.getId());

    assertThat(outbox.findByRecipientOrderByIdAsc("ana@example.com"))
        .filteredOn(m -> m.getSubject().startsWith("Tickets available"))
        .hasSize(2);
  }

  @Test
  void onlyOpenOffersCanBeSentAgain() {
    WaitlistEntry entry = join("Ana", 1);

    assertThatThrownBy(() -> waitlist.resendOffer(event.getId(), entry.getId()))
        .hasMessage("Only open offers can be sent again.");
  }

  @Test
  void nothingToOfferWhileSoldOut() {
    join("Ana", 1);

    assertThat(waitlist.offerReleasedTickets(event.getId())).isZero();
  }
}

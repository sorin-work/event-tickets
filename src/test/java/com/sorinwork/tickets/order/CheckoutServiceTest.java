package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.ticket.TicketRepository;
import com.sorinwork.tickets.waitlist.WaitlistEntry;
import com.sorinwork.tickets.waitlist.WaitlistForm;
import com.sorinwork.tickets.waitlist.WaitlistRepository;
import com.sorinwork.tickets.waitlist.WaitlistService;
import com.sorinwork.tickets.waitlist.WaitlistStatus;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckoutServiceTest extends IntegrationTest {

  @Autowired CheckoutService checkout;
  @Autowired EventService events;
  @Autowired TicketTierRepository tiers;
  @Autowired TicketRepository tickets;
  @Autowired OrderRepository orders;
  @Autowired AvailabilityService availability;
  @Autowired WaitlistService waitlist;
  @Autowired WaitlistRepository waitlistEntries;

  Event event;
  TicketTier standing;
  TicketTier vip;

  @BeforeEach
  void setUp() {
    event = data.event("Jazz Night");
    standing = data.tier(event, "Standing", "15", 10);
    vip = data.tier(event, "VIP", "40", 2);
    events.publish(event.getId());
  }

  private static Map<Long, Integer> select(Object... tierAndQuantity) {
    Map<Long, Integer> map = new LinkedHashMap<>();
    for (int i = 0; i < tierAndQuantity.length; i += 2) {
      map.put(((TicketTier) tierAndQuantity[i]).getId(), (Integer) tierAndQuantity[i + 1]);
    }
    return map;
  }

  @Test
  void holdsSeatsForTenMinutes() {
    Order order = checkout.start(event.getId(), select(standing, 2, vip, 1));

    assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(order.getHoldExpiresAt()).isEqualTo(clock.instant().plus(Duration.ofMinutes(10)));
    assertThat(order.getSubtotalCents()).isEqualTo(2 * 1500 + 4000);
    assertThat(order.getTotalCents()).isEqualTo(7000);
    assertThat(order.ticketCount()).isEqualTo(3);
    assertThat(order.getReference()).startsWith("ET-");
  }

  @Test
  void ignoresTiersWithZeroTickets() {
    Order order = checkout.start(event.getId(), select(standing, 1, vip, 0));

    assertThat(order.getItems()).hasSize(1);
  }

  @Test
  void rejectsNegativeQuantities() {
    assertThatThrownBy(() -> checkout.start(event.getId(), select(vip, 2, standing, -3)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Ticket quantities cannot be negative.");
    assertThatThrownBy(() -> checkout.start(event.getId(), select(standing, -1)))
        .hasMessage("Ticket quantities cannot be negative.");
  }

  @Test
  void requiresAtLeastOneTicket() {
    assertThatThrownBy(() -> checkout.start(event.getId(), select(standing, 0)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("Choose at least one ticket.");
  }

  @Test
  void heldSeatsAreNotAvailableToOthers() {
    checkout.start(event.getId(), select(vip, 2));

    assertThatThrownBy(() -> checkout.start(event.getId(), select(vip, 1)))
        .isInstanceOf(BusinessException.class)
        .hasMessage("VIP is sold out.");
  }

  @Test
  void seatsComeBackWhenTheHoldRunsOut() {
    checkout.start(event.getId(), select(vip, 2));
    clock.advance(Duration.ofMinutes(11));

    assertThat(checkout.start(event.getId(), select(vip, 2)).ticketCount()).isEqualTo(2);
  }

  @Test
  void tellsHowManyAreLeft() {
    checkout.start(event.getId(), select(standing, 8));

    assertThatThrownBy(() -> checkout.start(event.getId(), select(standing, 3)))
        .hasMessage("Only 2 Standing left.");
  }

  @Test
  void enforcesMaxPerOrder() {
    assertThatThrownBy(() -> checkout.start(event.getId(), select(standing, 11)))
        .hasMessageContaining("at most 10");
  }

  @Test
  void salesCloseWhenTheEventStarts() {
    // 2030-03-14 19:30 in Bucharest is 17:30 UTC
    clock.set(java.time.Instant.parse("2030-03-14T17:30:00Z"));

    assertThatThrownBy(() -> checkout.start(event.getId(), select(standing, 1)))
        .hasMessageContaining("sales have closed");
  }

  @Test
  void rejectsDraftEvents() {
    Event draft = data.event("Draft");
    TicketTier tier = data.tier(draft, "Entry", "5", 10);

    assertThatThrownBy(() -> checkout.start(draft.getId(), select(tier, 1)))
        .hasMessageContaining("not on sale");
  }

  @Test
  void rejectsTiersOfOtherEvents() {
    Event other = data.event("Other");
    TicketTier foreign = data.tier(other, "Entry", "5", 10);

    assertThatThrownBy(() -> checkout.start(event.getId(), select(foreign, 1)))
        .hasMessage("Unknown ticket type.");
  }

  @Test
  void salesWindowsUseTheEventTimeZone() {
    // The clock is 10:00 UTC, which is 12:00 in Bucharest.
    standing.setSalesEnd(LocalDateTime.of(2030, 1, 15, 11, 0));
    tiers.save(standing);
    vip.setSalesStart(LocalDateTime.of(2030, 1, 15, 11, 30));
    tiers.save(vip);

    assertThatThrownBy(() -> checkout.start(event.getId(), select(standing, 1)))
        .hasMessage("Sales for Standing have ended.");
    assertThat(checkout.start(event.getId(), select(vip, 1)).ticketCount()).isEqualTo(1);
  }

  @Test
  void farAwayVenuesOpenSalesInTheirOwnMorning() {
    Event tokyo = data.event("Tokyo Meetup");
    var form = com.sorinwork.tickets.support.TestData.eventForm("Tokyo Meetup");
    form.setTimeZone("Asia/Tokyo");
    events.update(tokyo.getId(), form);
    TicketTier seat = data.tier(tokyo, "Seat", "0", 10);
    // 09:00 in Tokyo is 00:00 UTC; the clock is at 10:00 UTC = 19:00 Tokyo.
    seat.setSalesStart(LocalDateTime.of(2030, 1, 15, 18, 0));
    tiers.save(seat);
    events.publish(tokyo.getId());

    assertThat(checkout.start(tokyo.getId(), select(seat, 1)).ticketCount()).isEqualTo(1);
  }

  @Test
  void respectsSalesWindow() {
    standing.setSalesStart(LocalDateTime.of(2030, 2, 1, 10, 0));
    tiers.save(standing);
    vip.setSalesEnd(LocalDateTime.of(2030, 1, 1, 0, 0));
    tiers.save(vip);

    assertThatThrownBy(() -> checkout.start(event.getId(), select(standing, 1)))
        .hasMessage("Standing is not on sale yet.");
    assertThatThrownBy(() -> checkout.start(event.getId(), select(vip, 1)))
        .hasMessage("Sales for VIP have ended.");
  }

  @Test
  void completingCheckoutIssuesOneTicketPerSeat() {
    Order order = checkout.start(event.getId(), select(standing, 2, vip, 1));

    Order paid =
        checkout.complete(order.getAccessToken(), new CheckoutForm(" Ana Pop ", "Ana@Example.com"));

    assertThat(paid.getStatus()).isEqualTo(OrderStatus.PAID);
    assertThat(paid.getPaidAt()).isEqualTo(clock.instant());
    assertThat(paid.getBuyerEmail()).isEqualTo("ana@example.com");
    List<Ticket> issued = tickets.findByOrderIdOrderById(order.getId());
    assertThat(issued)
        .hasSize(3)
        .allSatisfy(t -> assertThat(t.getAttendeeName()).isEqualTo("Ana Pop"));
    assertThat(issued).extracting(Ticket::getCode).doesNotHaveDuplicates();
  }

  @Test
  void paidSeatsStayTakenAfterTheHoldTime() {
    Order order = checkout.start(event.getId(), select(vip, 2));
    checkout.complete(order.getAccessToken(), new CheckoutForm("Ana", "ana@example.com"));
    clock.advance(Duration.ofHours(1));

    assertThatThrownBy(() -> checkout.start(event.getId(), select(vip, 1)))
        .hasMessage("VIP is sold out.");
  }

  @Test
  void cannotCompleteAfterTheHoldRanOut() {
    Order order = checkout.start(event.getId(), select(vip, 1));
    clock.advance(Duration.ofMinutes(10));

    assertThatThrownBy(
            () -> checkout.complete(order.getAccessToken(), new CheckoutForm("Ana", "a@b.co")))
        .hasMessageContaining("expired");
  }

  @Test
  void completingTwiceDoesNotIssueMoreTickets() {
    Order order = checkout.start(event.getId(), select(standing, 2));
    checkout.complete(order.getAccessToken(), new CheckoutForm("Ana", "ana@example.com"));

    assertThatThrownBy(
            () ->
                checkout.complete(
                    order.getAccessToken(), new CheckoutForm("Ana", "ana@example.com")))
        .isInstanceOf(BusinessException.class)
        .hasMessage("This order has already been paid.");
    assertThat(tickets.findByOrderIdOrderById(order.getId())).hasSize(2);
  }

  @Test
  void cannotCompleteACancelledCheckout() {
    Order order = checkout.start(event.getId(), select(vip, 1));
    Order stored = checkout.getByToken(order.getAccessToken());
    stored.setStatus(OrderStatus.CANCELLED);
    orders.save(stored);

    assertThatThrownBy(
            () -> checkout.complete(order.getAccessToken(), new CheckoutForm("Ana", "a@b.co")))
        .hasMessageContaining("expired");
  }

  private int vipLeft() {
    return availability.forEvent(event.getId()).get(vip.getId()).remaining();
  }

  @Test
  void releasingAHoldFreesItsSeatsAtOnce() {
    Order order = checkout.start(event.getId(), select(vip, 2));
    assertThat(vipLeft()).isZero();

    Order released = checkout.release(order.getAccessToken());

    assertThat(released.getStatus()).isEqualTo(OrderStatus.EXPIRED);
    assertThat(vipLeft()).isEqualTo(2);
  }

  @Test
  void releasingTwiceIsHarmless() {
    String token = checkout.start(event.getId(), select(vip, 1)).getAccessToken();
    checkout.release(token);

    assertThat(checkout.release(token).getStatus()).isEqualTo(OrderStatus.EXPIRED);
    assertThat(vipLeft()).isEqualTo(2);
  }

  @Test
  void releasingAPaidOrderDoesNothing() {
    Order paid = purchases.buy(vip, 1, "Ana", "ana@example.com");

    assertThat(checkout.release(paid.getAccessToken()).getStatus()).isEqualTo(OrderStatus.PAID);
    assertThat(vipLeft()).isEqualTo(1);
  }

  @Test
  void startingAgainFromTheSameBrowserCanTakeTheSameSeats() {
    String first = checkout.start(event.getId(), select(vip, 2)).getAccessToken();

    Order second = checkout.start(event.getId(), select(vip, 2), first);

    assertThat(second.ticketCount()).isEqualTo(2);
    assertThat(checkout.getByToken(first).getStatus()).isEqualTo(OrderStatus.EXPIRED);
    assertThat(vipLeft()).isZero();
  }

  @Test
  void aRefusedNewSelectionKeepsThePreviousHold() {
    String first = checkout.start(event.getId(), select(vip, 2)).getAccessToken();

    assertThatThrownBy(() -> checkout.start(event.getId(), select(vip, 3), first))
        .isInstanceOf(BusinessException.class);

    assertThat(checkout.getByToken(first).getStatus()).isEqualTo(OrderStatus.PENDING);
    assertThat(vipLeft()).isZero();
  }

  @Test
  void aCheckoutOfAnotherEventIsNotReplaced() {
    Event other = data.event("Other Night");
    TicketTier seat = data.tier(other, "Seat", "10", 5);
    events.publish(other.getId());
    String elsewhere = checkout.start(other.getId(), select(seat, 1)).getAccessToken();

    checkout.start(event.getId(), select(vip, 1), elsewhere);

    assertThat(checkout.getByToken(elsewhere).getStatus()).isEqualTo(OrderStatus.PENDING);
  }

  @Test
  void onlySeatsTheNewSelectionLeavesGoToTheWaitlist() {
    String first = checkout.start(event.getId(), select(vip, 2)).getAccessToken();
    WaitlistEntry ben =
        waitlist.join(
            events.get(event.getId()), new WaitlistForm(vip.getId(), "Ben", "ben@example.com", 1));

    Order second = checkout.start(event.getId(), select(vip, 1), first);

    assertThat(second.ticketCount()).isEqualTo(1);
    assertThat(waitlistEntries.findById(ben.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.OFFERED);
    assertThat(vipLeft()).isZero();
  }
}

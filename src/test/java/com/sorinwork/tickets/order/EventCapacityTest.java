package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventForm;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.waitlist.WaitlistEntry;
import com.sorinwork.tickets.waitlist.WaitlistForm;
import com.sorinwork.tickets.waitlist.WaitlistRepository;
import com.sorinwork.tickets.waitlist.WaitlistService;
import com.sorinwork.tickets.waitlist.WaitlistStatus;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventCapacityTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired EventRepository eventRepository;
  @Autowired WaitlistService waitlist;
  @Autowired WaitlistRepository waitlistEntries;
  @Autowired CheckoutService checkout;
  @Autowired AvailabilityService availability;

  Event event;
  TicketTier standing;
  TicketTier seated;

  @BeforeEach
  void setUp() {
    event = data.event("Basement Show");
    standing = data.tier(event, "Standing", "10", 8);
    seated = data.tier(event, "Seated", "15", 8);
    Event stored = eventRepository.findById(event.getId()).orElseThrow();
    stored.setCapacity(10);
    eventRepository.save(stored);
    events.publish(event.getId());
  }

  @Test
  void tiersShareTheVenueCapacity() {
    checkout.start(event.getId(), Map.of(standing.getId(), 7));

    Map<Long, TierAvailability> left = availability.forEvent(event.getId());
    assertThat(left.get(standing.getId()).remaining()).isEqualTo(1);
    assertThat(left.get(seated.getId()).remaining()).isEqualTo(3);
  }

  @Test
  void checkoutStopsAtTheVenueCapacity() {
    purchases.buy(standing, 6, "Ana", "ana@example.com");

    assertThatThrownBy(() -> checkout.start(event.getId(), Map.of(seated.getId(), 5)))
        .hasMessage("Only 4 Seated left.");
  }

  @Test
  void remainingForSeveralEventsMatchesThePerEventFigures() {
    purchases.buy(standing, 3, "Ana", "ana@example.com");
    checkout.start(event.getId(), Map.of(seated.getId(), 2));
    Event other = data.event("Other");
    TicketTier open = data.tier(other, "Open", "0", 50);
    events.publish(other.getId());
    purchases.buy(open, 5, "Bob", "bob@example.com");
    Event empty = data.event("No Tiers Yet");

    Map<Long, Integer> left =
        availability.remainingPerEvent(
            java.util.List.of(event.getId(), other.getId(), empty.getId()));

    // venue capacity 10: 3 sold + 2 held leave 5 across both tiers
    assertThat(left.get(event.getId())).isEqualTo(5);
    assertThat(left.get(other.getId())).isEqualTo(45);
    assertThat(left.get(empty.getId())).isZero();
  }

  @Test
  void fullVenueSellsOutEveryTier() {
    purchases.buy(standing, 5, "Ana", "ana@example.com");
    purchases.buy(seated, 5, "Bogdan", "bogdan@example.com");

    assertThat(availability.forEvent(event.getId()).values())
        .allSatisfy(a -> assertThat(a.isSoldOut()).isTrue());
  }

  private Event withCapacity(Integer capacity) {
    EventForm form = EventForm.from(events.get(event.getId()));
    form.setCapacity(capacity);
    return events.update(event.getId(), form);
  }

  @Test
  void capacityBelowWhatIsSoldIsRefused() {
    purchases.buy(standing, 6, "Ana", "ana@example.com");

    assertThatThrownBy(() -> withCapacity(5)).hasMessage("That is fewer than are taken: 6 sold.");
    assertThat(events.get(event.getId()).getCapacity()).isEqualTo(10);
  }

  @Test
  void capacityBelowSoldAndHeldIsRefused() {
    purchases.buy(standing, 4, "Ana", "ana@example.com");
    checkout.start(event.getId(), Map.of(seated.getId(), 3));

    assertThatThrownBy(() -> withCapacity(6))
        .hasMessage(
            "That is fewer than are taken: 4 sold and 3 held in open checkouts,"
                + " which end by 12:10.");
  }

  @Test
  void capacityOfExactlyWhatIsTakenIsAccepted() {
    purchases.buy(standing, 4, "Ana", "ana@example.com");
    checkout.start(event.getId(), Map.of(seated.getId(), 3));

    assertThat(withCapacity(7).getCapacity()).isEqualTo(7);
  }

  @Test
  void raisingTheCapacityOffersTheNewSeatsToTheWaitlist() {
    purchases.buy(standing, 8, "Ana", "ana@example.com");
    purchases.buy(seated, 2, "Bogdan", "bogdan@example.com");
    WaitlistEntry dana =
        waitlist.join(
            events.get(event.getId()),
            new WaitlistForm(seated.getId(), "Dana", "dana@example.com", 1));

    withCapacity(11);

    assertThat(waitlistEntries.findById(dana.getId()).orElseThrow().getStatus())
        .isEqualTo(WaitlistStatus.OFFERED);
  }
}

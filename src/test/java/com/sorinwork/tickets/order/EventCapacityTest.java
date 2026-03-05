package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventCapacityTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired EventRepository eventRepository;
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
  void fullVenueSellsOutEveryTier() {
    purchases.buy(standing, 5, "Ana", "ana@example.com");
    purchases.buy(seated, 5, "Bogdan", "bogdan@example.com");

    assertThat(availability.forEvent(event.getId()).values())
        .allSatisfy(a -> assertThat(a.isSoldOut()).isTrue());
  }
}

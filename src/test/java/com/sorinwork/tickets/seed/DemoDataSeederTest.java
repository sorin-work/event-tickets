package com.sorinwork.tickets.seed;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TierService;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.promo.PromoCodeService;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.CheckInService;
import com.sorinwork.tickets.ticket.TicketRepository;
import com.sorinwork.tickets.waitlist.WaitlistRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

class DemoDataSeederTest extends IntegrationTest {

  @Autowired EventRepository eventRepository;
  @Autowired EventService events;
  @Autowired TierService tiers;
  @Autowired PromoCodeService promos;
  @Autowired CheckoutService checkout;
  @Autowired CheckInService checkIn;
  @Autowired com.sorinwork.tickets.waitlist.WaitlistService waitlist;
  @Autowired TicketRepository tickets;
  @Autowired WaitlistRepository waitlistEntries;
  @Autowired TransactionTemplate transactions;

  private DemoDataSeeder seeder() {
    return new DemoDataSeeder(
        eventRepository, events, tiers, promos, checkout, checkIn, waitlist, clock);
  }

  @Test
  void seedsAnEmptyDatabaseOnce() {
    transactions.executeWithoutResult(status -> seeder().run(null));
    transactions.executeWithoutResult(status -> seeder().run(null));

    assertThat(eventRepository.count()).isEqualTo(5);
    assertThat(tickets.count()).isGreaterThan(200);
    assertThat(waitlistEntries.count()).isEqualTo(4);
    assertThat(eventRepository.findBySlug("summer-jazz-night")).isPresent();
  }

  @Test
  void pastArrivalsAreSpreadOverAnHourAndAHalf() {
    transactions.executeWithoutResult(status -> seeder().run(null));

    var past =
        eventRepository.findAll().stream()
            .filter(e -> e.startsAtInstant().isBefore(clock.instant()))
            .findFirst()
            .orElseThrow();
    List<Instant> arrivals = tickets.checkInTimes(past.getId());
    assertThat(arrivals).hasSize(52);
    assertThat(arrivals).allSatisfy(at -> assertThat(at).isBefore(clock.instant()));
    assertThat(Duration.between(arrivals.get(0), arrivals.get(51)))
        .isGreaterThan(Duration.ofMinutes(80));
  }
}

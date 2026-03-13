package com.sorinwork.tickets.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.support.IntegrationTest;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class EventCopierTest extends IntegrationTest {

  @Autowired EventCopier copier;
  @Autowired EventService events;
  @Autowired TierService tiers;
  @Autowired TicketTierRepository tierRepository;

  @Test
  void copiesTheEventAndItsTiersAsADraft() {
    Event source = data.event("Monthly Meetup");
    TicketTier tier = data.tier(source, "Attendee", "0", 40);
    tier.setSalesEnd(LocalDateTime.of(2030, 3, 14, 18, 0));
    tierRepository.save(tier);
    data.tier(source, "Supporter", "10", 10);
    events.publish(source.getId());

    Event copy = copier.duplicate(source.getId());

    assertThat(copy.getTitle()).isEqualTo("Copy of Monthly Meetup");
    assertThat(copy.getSlug()).isEqualTo("copy-of-monthly-meetup");
    assertThat(copy.getStatus()).isEqualTo(EventStatus.DRAFT);
    assertThat(copy.getVenueName()).isEqualTo(source.getVenueName());
    assertThat(tiers.forEvent(copy.getId()))
        .extracting(TicketTier::getName, TicketTier::getPriceCents, TicketTier::getSalesEnd)
        .containsExactly(
            org.assertj.core.groups.Tuple.tuple("Attendee", 0L, null),
            org.assertj.core.groups.Tuple.tuple("Supporter", 1000L, null));
  }

  @Test
  void keepsLongTitlesWithinTheLimit() {
    assertThat(EventCopier.copyTitle("x".repeat(160))).hasSize(160);
  }
}

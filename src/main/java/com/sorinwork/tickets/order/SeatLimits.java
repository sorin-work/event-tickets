package com.sorinwork.tickets.order;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.TicketTier;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Checks a new tier quantity or venue capacity against what is already sold and held. Callers hold
 * the event lock ({@code EventRepository.lockById}). Starting a checkout, paying and giving seats
 * back take the same lock, and paying judges the hold only once it has the lock, so sold plus held
 * cannot change between this check and the save.
 *
 * <p>Held seats count as taken: every open checkout can still be paid, so a limit below sold plus
 * held could be exceeded a minute later.
 */
@Component
public class SeatLimits {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");
  private static final DateTimeFormatter DAY_AND_TIME =
      DateTimeFormatter.ofPattern("EEE d MMM, HH:mm", Locale.UK);

  private final AvailabilityService availability;
  private final OrderItemRepository items;
  private final Clock clock;

  public SeatLimits(AvailabilityService availability, OrderItemRepository items, Clock clock) {
    this.availability = availability;
    this.items = items;
    this.clock = clock;
  }

  public void checkTierQuantity(TicketTier tier, int quantity) {
    TierAvailability current = availability.forEvent(tier.getEvent().getId()).get(tier.getId());
    if (current == null || quantity >= current.sold() + current.held()) {
      return;
    }
    Instant holdEnd = items.lastHoldEndForTier(tier.getId(), clock.instant(), OrderStatus.PENDING);
    throw new BusinessException(tooLow(tier.getEvent(), current.sold(), current.held(), holdEnd));
  }

  public void checkCapacity(Event event, Integer capacity) {
    if (capacity == null) {
      return;
    }
    Map<Long, TierAvailability> tiers = availability.forEvent(event.getId());
    int sold = tiers.values().stream().mapToInt(TierAvailability::sold).sum();
    int held = tiers.values().stream().mapToInt(TierAvailability::held).sum();
    if (capacity >= sold + held) {
      return;
    }
    Instant holdEnd =
        items.lastHoldEndForEvent(event.getId(), clock.instant(), OrderStatus.PENDING);
    throw new BusinessException(tooLow(event, sold, held, holdEnd));
  }

  private String tooLow(Event event, int sold, int held, Instant holdEnd) {
    String message = "That is fewer than are taken: " + sold + " sold";
    if (held > 0 && holdEnd != null) {
      message += " and " + held + " held in open checkouts, which end by " + when(event, holdEnd);
    }
    return message + ".";
  }

  /** "19:42" today at the venue, "Thu 16 Jan, 09:30" on another day (waitlist offers last 24h). */
  private String when(Event event, Instant at) {
    ZonedDateTime local = at.atZone(event.zone());
    LocalDate today = LocalDate.now(clock.withZone(event.zone()));
    return local.toLocalDate().equals(today) ? TIME.format(local) : DAY_AND_TIME.format(local);
  }
}

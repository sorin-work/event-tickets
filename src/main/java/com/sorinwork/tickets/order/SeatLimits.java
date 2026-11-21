package com.sorinwork.tickets.order;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.TicketTier;
import java.time.Clock;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Component;

/**
 * Checks a new tier quantity or venue capacity against what is already sold and held. Callers hold
 * the event lock ({@code EventRepository.lockById}), the same lock checkout takes, so the numbers
 * cannot change between this check and the save.
 *
 * <p>Held seats count as taken: every open checkout can still be paid, so a limit below sold plus
 * held could be exceeded a minute later.
 */
@Component
public class SeatLimits {

  private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm");

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

  private static String tooLow(Event event, int sold, int held, Instant holdEnd) {
    String message = "That is fewer than are taken: " + sold + " sold";
    if (held > 0 && holdEnd != null) {
      message +=
          " and "
              + held
              + " held in open checkouts, which end by "
              + TIME.format(holdEnd.atZone(event.zone()));
    }
    return message + ".";
  }
}

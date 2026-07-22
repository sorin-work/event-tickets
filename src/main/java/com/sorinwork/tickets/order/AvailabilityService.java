package com.sorinwork.tickets.order;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import java.time.Clock;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AvailabilityService {

  private final EventRepository events;
  private final TicketTierRepository tiers;
  private final OrderItemRepository items;
  private final Clock clock;

  public AvailabilityService(
      EventRepository events, TicketTierRepository tiers, OrderItemRepository items, Clock clock) {
    this.events = events;
    this.tiers = tiers;
    this.items = items;
    this.clock = clock;
  }

  /**
   * Tickets left per event id for a whole list of events, e.g. the home page, in three queries
   * instead of three per event. Same rules as {@link #forEvent}.
   */
  public Map<Long, Integer> remainingPerEvent(java.util.Collection<Long> eventIds) {
    Map<Long, Integer> result = new HashMap<>();
    if (eventIds.isEmpty()) {
      return result;
    }
    Map<Long, OrderItemRepository.TierCounts> counts = new HashMap<>();
    for (var row :
        items.countsForEvents(eventIds, clock.instant(), OrderStatus.PAID, OrderStatus.PENDING)) {
      counts.put(row.getTierId(), row);
    }
    Map<Long, List<TicketTier>> byEvent = new HashMap<>();
    for (TicketTier tier : tiers.findByEventIdIn(eventIds)) {
      byEvent.computeIfAbsent(tier.getEvent().getId(), id -> new java.util.ArrayList<>()).add(tier);
    }
    for (Long eventId : eventIds) {
      List<TicketTier> eventTiers = byEvent.getOrDefault(eventId, List.of());
      int taken = 0;
      Integer capacity = null;
      for (TicketTier tier : eventTiers) {
        var row = counts.get(tier.getId());
        if (row != null) {
          taken += row.getSold().intValue() + row.getHeld().intValue();
        }
        capacity = tier.getEvent().getCapacity();
      }
      int eventRemaining = capacity == null ? Integer.MAX_VALUE : Math.max(0, capacity - taken);
      int left = 0;
      for (TicketTier tier : eventTiers) {
        var row = counts.get(tier.getId());
        int sold = row == null ? 0 : row.getSold().intValue();
        int held = row == null ? 0 : row.getHeld().intValue();
        left +=
            new TierAvailability(tier.getId(), tier.getQuantity(), sold, held, eventRemaining)
                .remaining();
      }
      result.put(eventId, capacity == null ? left : Math.min(left, eventRemaining));
    }
    return result;
  }

  /**
   * Availability of every tier of the event, keyed by tier id, in display order. When the event has
   * a venue capacity no tier can offer more than what is left of it.
   */
  public Map<Long, TierAvailability> forEvent(long eventId) {
    Map<Long, OrderItemRepository.TierCounts> counts = new HashMap<>();
    for (var row :
        items.countsForEvent(eventId, clock.instant(), OrderStatus.PAID, OrderStatus.PENDING)) {
      counts.put(row.getTierId(), row);
    }
    List<TicketTier> eventTiers = tiers.findByEventIdOrderByPositionAscIdAsc(eventId);
    int taken = 0;
    for (var row : counts.values()) {
      taken += row.getSold().intValue() + row.getHeld().intValue();
    }
    Integer capacity = events.findById(eventId).map(Event::getCapacity).orElse(null);
    int eventRemaining = capacity == null ? Integer.MAX_VALUE : Math.max(0, capacity - taken);

    Map<Long, TierAvailability> result = new LinkedHashMap<>();
    for (TicketTier tier : eventTiers) {
      var row = counts.get(tier.getId());
      int sold = row == null ? 0 : row.getSold().intValue();
      int held = row == null ? 0 : row.getHeld().intValue();
      result.put(
          tier.getId(),
          new TierAvailability(tier.getId(), tier.getQuantity(), sold, held, eventRemaining));
    }
    return result;
  }
}

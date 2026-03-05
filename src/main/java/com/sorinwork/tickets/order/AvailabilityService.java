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

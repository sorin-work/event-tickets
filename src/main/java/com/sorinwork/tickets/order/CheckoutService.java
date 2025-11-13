package com.sorinwork.tickets.order;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.common.Tokens;
import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns a ticket selection into an order that holds the seats while the buyer checks out. */
@Service
@Transactional
public class CheckoutService {

  private final EventRepository events;
  private final TicketTierRepository tiers;
  private final OrderRepository orders;
  private final AvailabilityService availability;
  private final AppProperties properties;
  private final Clock clock;

  public CheckoutService(
      EventRepository events,
      TicketTierRepository tiers,
      OrderRepository orders,
      AvailabilityService availability,
      AppProperties properties,
      Clock clock) {
    this.events = events;
    this.tiers = tiers;
    this.orders = orders;
    this.availability = availability;
    this.properties = properties;
    this.clock = clock;
  }

  /**
   * Holds the selected tickets.
   *
   * @param quantities tier id to number of tickets; zero entries are ignored
   */
  public Order start(long eventId, Map<Long, Integer> quantities) {
    Event event =
        events.lockById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId));
    if (!event.isPublished()) {
      throw new BusinessException("Tickets for this event are not on sale.");
    }
    Instant now = clock.instant();
    Map<Long, TierAvailability> available = availability.forEvent(eventId);
    Order order =
        new Order(
            event,
            Tokens.orderReference(),
            Tokens.accessToken(),
            now,
            now.plus(properties.checkout().holdDuration()));

    for (var entry : quantities.entrySet()) {
      int quantity = entry.getValue() == null ? 0 : entry.getValue();
      if (quantity == 0) {
        continue;
      }
      TicketTier tier =
          tiers
              .findByIdAndEventId(entry.getKey(), eventId)
              .orElseThrow(() -> new BusinessException("Unknown ticket type."));
      checkOnSale(tier);
      if (quantity > tier.getMaxPerOrder()) {
        throw new BusinessException(
            "You can buy at most " + tier.getMaxPerOrder() + " " + tier.getName() + " tickets.");
      }
      TierAvailability tierAvailability = available.get(tier.getId());
      if (quantity > tierAvailability.remaining()) {
        throw new BusinessException(
            tierAvailability.isSoldOut()
                ? tier.getName() + " is sold out."
                : "Only " + tierAvailability.remaining() + " " + tier.getName() + " left.");
      }
      order.addItem(new OrderItem(order, tier, quantity));
    }
    if (order.getItems().isEmpty()) {
      throw new BusinessException("Choose at least one ticket.");
    }
    return orders.save(order);
  }

  private void checkOnSale(TicketTier tier) {
    LocalDateTime now = LocalDateTime.now(clock);
    if (tier.getSalesStart() != null && now.isBefore(tier.getSalesStart())) {
      throw new BusinessException(tier.getName() + " is not on sale yet.");
    }
    if (tier.getSalesEnd() != null && !now.isBefore(tier.getSalesEnd())) {
      throw new BusinessException("Sales for " + tier.getName() + " have ended.");
    }
  }
}

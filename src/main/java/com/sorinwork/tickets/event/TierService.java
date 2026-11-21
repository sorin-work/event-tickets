package com.sorinwork.tickets.event;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.order.OrderEvents;
import com.sorinwork.tickets.order.SeatLimits;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TierService {

  private final TicketTierRepository tiers;
  private final EventService events;
  private final EventRepository eventRows;
  private final SeatLimits limits;
  private final ApplicationEventPublisher publisher;
  private final Clock clock;

  public TierService(
      TicketTierRepository tiers,
      EventService events,
      EventRepository eventRows,
      SeatLimits limits,
      ApplicationEventPublisher publisher,
      Clock clock) {
    this.tiers = tiers;
    this.events = events;
    this.eventRows = eventRows;
    this.limits = limits;
    this.publisher = publisher;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<TicketTier> forEvent(long eventId) {
    return tiers.findByEventIdOrderByPositionAscIdAsc(eventId);
  }

  @Transactional(readOnly = true)
  public TicketTier get(long eventId, long tierId) {
    return tiers
        .findByIdAndEventId(tierId, eventId)
        .orElseThrow(() -> new NotFoundException("Tier " + tierId));
  }

  public TicketTier create(long eventId, TierForm form) {
    Event event = events.get(eventId);
    TicketTier tier =
        new TicketTier(
            event, form.getName().trim(), form.priceCents(), form.getQuantity(), clock.instant());
    apply(tier, form);
    tier.setPosition(tiers.countByEventId(eventId));
    return tiers.save(tier);
  }

  /**
   * Saves a tier. Under the event lock, like checkout: a quantity below what is sold and held is
   * refused, and seats that come back on sale (more tickets, sales resumed) go to the waitlist
   * first.
   */
  public TicketTier update(long eventId, long tierId, TierForm form) {
    eventRows.lockById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId));
    TicketTier tier = get(eventId, tierId);
    limits.checkTierQuantity(tier, form.getQuantity());
    boolean freesSeats =
        form.getQuantity() > tier.getQuantity() || (tier.isPaused() && !form.isPaused());
    tier.setName(form.getName().trim());
    tier.setPriceCents(form.priceCents());
    tier.setQuantity(form.getQuantity());
    apply(tier, form);
    if (freesSeats && !tier.isPaused()) {
      tiers.flush();
      publisher.publishEvent(new OrderEvents.TicketsReleased(eventId));
    }
    return tier;
  }

  /** Removes a tier nobody has bought or reserved; otherwise it can only be paused. */
  public void delete(long eventId, long tierId) {
    TicketTier tier = get(eventId, tierId);
    if (tiers.countOrderItems(tierId) > 0) {
      throw new BusinessException(
          tier.getName() + " has orders and cannot be removed. Pause its sales instead.");
    }
    tiers.delete(tier);
  }

  /** Moves a tier one place up ({@code -1}) or down ({@code +1}) on the event page. */
  public void move(long eventId, long tierId, int direction) {
    List<TicketTier> ordered = new ArrayList<>(forEvent(eventId));
    int index = -1;
    for (int i = 0; i < ordered.size(); i++) {
      if (ordered.get(i).getId() == tierId) {
        index = i;
      }
    }
    if (index < 0) {
      throw new NotFoundException("Tier " + tierId);
    }
    int target = index + Integer.signum(direction);
    if (target >= 0 && target < ordered.size()) {
      Collections.swap(ordered, index, target);
    }
    for (int i = 0; i < ordered.size(); i++) {
      ordered.get(i).setPosition(i);
    }
  }

  private void apply(TicketTier tier, TierForm form) {
    String description = form.getDescription();
    tier.setDescription(description == null || description.isBlank() ? null : description.trim());
    tier.setMaxPerOrder(form.getMaxPerOrder());
    tier.setSalesStart(form.getSalesStart());
    tier.setSalesEnd(form.getSalesEnd());
    tier.setPaused(form.isPaused());
  }
}

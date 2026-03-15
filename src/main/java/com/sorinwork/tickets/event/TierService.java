package com.sorinwork.tickets.event;

import com.sorinwork.tickets.common.NotFoundException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TierService {

  private final TicketTierRepository tiers;
  private final EventService events;
  private final Clock clock;

  public TierService(TicketTierRepository tiers, EventService events, Clock clock) {
    this.tiers = tiers;
    this.events = events;
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

  public TicketTier update(long eventId, long tierId, TierForm form) {
    TicketTier tier = get(eventId, tierId);
    tier.setName(form.getName().trim());
    tier.setPriceCents(form.priceCents());
    tier.setQuantity(form.getQuantity());
    apply(tier, form);
    return tier;
  }

  public void delete(long eventId, long tierId) {
    tiers.delete(get(eventId, tierId));
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
  }
}

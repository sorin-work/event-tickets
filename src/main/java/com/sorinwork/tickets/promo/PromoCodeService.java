package com.sorinwork.tickets.promo;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTierRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PromoCodeService {

  private final PromoCodeRepository codes;
  private final EventService events;
  private final TicketTierRepository tiers;
  private final Clock clock;

  public PromoCodeService(
      PromoCodeRepository codes, EventService events, TicketTierRepository tiers, Clock clock) {
    this.codes = codes;
    this.events = events;
    this.tiers = tiers;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public List<PromoCode> forEvent(long eventId) {
    return codes.findByEventIdOrderByCode(eventId);
  }

  public PromoCode create(long eventId, PromoForm form) {
    Event event = events.get(eventId);
    String code = PromoCode.normalize(form.getCode());
    if (codes.existsByEventIdAndCode(eventId, code)) {
      throw new BusinessException("The code " + code + " already exists for this event.");
    }
    PromoCode promo =
        new PromoCode(event, code, form.getDiscountType(), form.amountValue(), clock.instant());
    promo.setMaxUses(form.getMaxUses());
    promo.setValidUntil(form.getValidUntil());
    if (form.getTierId() != null) {
      promo.setTier(
          tiers
              .findByIdAndEventId(form.getTierId(), eventId)
              .orElseThrow(() -> new BusinessException("Unknown ticket tier.")));
    }
    return codes.save(promo);
  }

  /** Changes how often and until when a code can be used. {@code null} means no limit. */
  public PromoCode updateLimits(
      long eventId, long promoId, Integer maxUses, java.time.LocalDateTime validUntil) {
    // Same lock as applying the code, so the count cannot change while the limit is checked.
    PromoCode promo = locked(eventId, promoId);
    long used = codes.countRedemptions(promoId, clock.instant());
    if (maxUses != null && maxUses < Math.max(1, used)) {
      throw new BusinessException(
          "Max uses cannot be lower than the " + used + " times it was used.");
    }
    promo.setMaxUses(maxUses);
    promo.setValidUntil(validUntil);
    return promo;
  }

  /** Current uses per promo code id: paid orders and open checkouts. */
  @Transactional(readOnly = true)
  public java.util.Map<Long, Long> usesForEvent(long eventId) {
    java.util.Map<Long, Long> uses = new java.util.HashMap<>();
    for (var row : codes.countRedemptionsForEvent(eventId, clock.instant())) {
      uses.put(row.getPromoId(), row.getUses());
    }
    return uses;
  }

  public PromoCode setActive(long eventId, long promoId, boolean active) {
    PromoCode promo = locked(eventId, promoId);
    promo.setActive(active);
    return promo;
  }

  private PromoCode locked(long eventId, long promoId) {
    return codes
        .lockByIdAndEventId(promoId, eventId)
        .orElseThrow(() -> new NotFoundException("Promo code " + promoId));
  }
}

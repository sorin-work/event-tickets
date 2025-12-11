package com.sorinwork.tickets.promo;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import java.time.Clock;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class PromoCodeService {

  private final PromoCodeRepository codes;
  private final EventService events;
  private final Clock clock;

  public PromoCodeService(PromoCodeRepository codes, EventService events, Clock clock) {
    this.codes = codes;
    this.events = events;
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
    return codes.save(promo);
  }

  public PromoCode setActive(long eventId, long promoId, boolean active) {
    PromoCode promo =
        codes
            .findByIdAndEventId(promoId, eventId)
            .orElseThrow(() -> new NotFoundException("Promo code " + promoId));
    promo.setActive(active);
    return promo;
  }
}

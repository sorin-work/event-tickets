package com.sorinwork.tickets.waitlist;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.order.AvailabilityService;
import java.time.Clock;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WaitlistService {

  static final List<WaitlistStatus> ACTIVE =
      List.of(WaitlistStatus.WAITING, WaitlistStatus.OFFERED);

  private final WaitlistRepository entries;
  private final TicketTierRepository tiers;
  private final AvailabilityService availability;
  private final Clock clock;

  public WaitlistService(
      WaitlistRepository entries,
      TicketTierRepository tiers,
      AvailabilityService availability,
      Clock clock) {
    this.entries = entries;
    this.tiers = tiers;
    this.availability = availability;
    this.clock = clock;
  }

  /** Puts someone in the queue for a sold-out tier. */
  public WaitlistEntry join(Event event, WaitlistForm form) {
    if (!event.isPublished()) {
      throw new BusinessException("Tickets for this event are not on sale.");
    }
    TicketTier tier =
        tiers
            .findByIdAndEventId(form.getTierId(), event.getId())
            .orElseThrow(() -> new BusinessException("Unknown ticket type."));
    if (!availability.forEvent(event.getId()).get(tier.getId()).isSoldOut()) {
      throw new BusinessException(tier.getName() + " is still on sale, no need to wait.");
    }
    if (form.getQuantity() > tier.getMaxPerOrder()) {
      throw new BusinessException(
          "You can ask for at most " + tier.getMaxPerOrder() + " " + tier.getName() + " tickets.");
    }
    String email = form.getEmail().trim().toLowerCase(Locale.ROOT);
    if (entries.existsByTierIdAndEmailAndStatusIn(tier.getId(), email, ACTIVE)) {
      throw new BusinessException("You are already on the waitlist for " + tier.getName() + ".");
    }
    return entries.save(
        new WaitlistEntry(tier, form.getName().trim(), email, form.getQuantity(), clock.instant()));
  }

  @Transactional(readOnly = true)
  public long waitingCount(long tierId) {
    return entries.countByTierIdAndStatus(tierId, WaitlistStatus.WAITING);
  }
}

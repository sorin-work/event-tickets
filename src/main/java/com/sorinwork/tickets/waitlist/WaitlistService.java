package com.sorinwork.tickets.waitlist;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.notify.Emails;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderEvents;
import com.sorinwork.tickets.order.TierAvailability;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class WaitlistService {

  static final List<WaitlistStatus> ACTIVE =
      List.of(WaitlistStatus.WAITING, WaitlistStatus.OFFERED);

  private final WaitlistRepository entries;
  private final TicketTierRepository tiers;
  private final EventRepository events;
  private final AvailabilityService availability;
  private final CheckoutService checkout;
  private final Emails emails;
  private final AppProperties properties;
  private final Clock clock;

  public WaitlistService(
      WaitlistRepository entries,
      TicketTierRepository tiers,
      EventRepository events,
      AvailabilityService availability,
      CheckoutService checkout,
      Emails emails,
      AppProperties properties,
      Clock clock) {
    this.entries = entries;
    this.tiers = tiers;
    this.events = events;
    this.availability = availability;
    this.checkout = checkout;
    this.emails = emails;
    this.properties = properties;
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
    WaitlistEntry entry =
        entries.save(
            new WaitlistEntry(
                tier, form.getName().trim(), email, form.getQuantity(), clock.instant()));
    emails.waitlistJoined(entry);
    return entry;
  }

  @Transactional(readOnly = true)
  public WaitlistEntry byLeaveToken(String token) {
    return entries
        .findByLeaveToken(token)
        .orElseThrow(() -> new NotFoundException("Waitlist entry"));
  }

  /** The person asked to be taken off the list. Has no effect once an offer was made. */
  public WaitlistEntry leave(String token) {
    WaitlistEntry entry = byLeaveToken(token);
    if (entry.getStatus() == WaitlistStatus.WAITING) {
      entry.setStatus(WaitlistStatus.REMOVED);
    }
    return entry;
  }

  /**
   * Offers tickets that became available again to the people waiting for them, in the order they
   * joined. Tickets are held for each person for the offer duration and they get an email with a
   * checkout link. Someone asking for more tickets than are free keeps their place; nobody behind
   * them is served first.
   *
   * @return how many people got an offer
   */
  public int offerReleasedTickets(long eventId) {
    Event event =
        events.lockById(eventId).orElseThrow(() -> new NotFoundException("Event " + eventId));
    if (!event.isPublished()) {
      return 0;
    }
    Map<Long, TierAvailability> available = availability.forEvent(eventId);
    Instant now = clock.instant();
    Instant until = now.plus(properties.waitlist().offerDuration());
    int offered = 0;
    for (TicketTier tier : tiers.findByEventIdOrderByPositionAscIdAsc(eventId)) {
      int remaining = available.get(tier.getId()).remaining();
      for (WaitlistEntry entry :
          entries.findByTierIdAndStatusOrderByCreatedAtAscIdAsc(
              tier.getId(), WaitlistStatus.WAITING)) {
        if (entry.getQuantity() > remaining) {
          break;
        }
        Order hold =
            checkout.holdForWaitlist(
                event, tier, entry.getQuantity(), entry.getName(), entry.getEmail(), until);
        entry.offer(hold, now);
        emails.waitlistOffer(entry, hold);
        remaining -= entry.getQuantity();
        offered++;
      }
    }
    return offered;
  }

  @EventListener
  public void onOrderPaid(OrderEvents.OrderPaid paid) {
    entries
        .findByOfferOrderId(paid.orderId())
        .ifPresent(entry -> entry.setStatus(WaitlistStatus.CLAIMED));
  }

  @EventListener
  public void onHoldExpired(OrderEvents.HoldExpired expired) {
    entries
        .findByOfferOrderId(expired.orderId())
        .filter(entry -> entry.getStatus() == WaitlistStatus.OFFERED)
        .ifPresent(entry -> entry.setStatus(WaitlistStatus.EXPIRED));
  }

  @EventListener
  public void onTicketsReleased(OrderEvents.TicketsReleased released) {
    offerReleasedTickets(released.eventId());
  }

  /** Sends the offer email again, e.g. when the person says they never got it. */
  public WaitlistEntry resendOffer(long eventId, long entryId) {
    WaitlistEntry entry =
        entries
            .findById(entryId)
            .filter(e -> e.getEvent().getId().equals(eventId))
            .orElseThrow(() -> new NotFoundException("Waitlist entry " + entryId));
    if (entry.getStatus() != WaitlistStatus.OFFERED) {
      throw new BusinessException("Only open offers can be sent again.");
    }
    emails.waitlistOffer(entry, entry.getOfferOrder());
    return entry;
  }

  /** Takes someone off the waitlist. Only people still waiting can be removed. */
  public WaitlistEntry remove(long eventId, long entryId) {
    WaitlistEntry entry =
        entries
            .findById(entryId)
            .filter(e -> e.getEvent().getId().equals(eventId))
            .orElseThrow(() -> new NotFoundException("Waitlist entry " + entryId));
    if (entry.getStatus() != WaitlistStatus.WAITING) {
      throw new BusinessException("Only people who are still waiting can be removed.");
    }
    entry.setStatus(WaitlistStatus.REMOVED);
    return entry;
  }

  /** Everyone who joined the waitlist of the event, in the order they joined. */
  @Transactional(readOnly = true)
  public List<WaitlistEntry> forEvent(long eventId) {
    return entries.findByEventIdOrderByCreatedAtAscIdAsc(eventId);
  }

  /** 1 for the next person to get an offer for the tier. */
  @Transactional(readOnly = true)
  public long position(WaitlistEntry entry) {
    return entries.countAheadOrEqual(
        entry.getTier().getId(), WaitlistStatus.WAITING, entry.getCreatedAt(), entry.getId());
  }

  /** Number of people waiting per tier id; tiers without anyone waiting are left out. */
  @Transactional(readOnly = true)
  public Map<Long, Long> waitingPerTier(long eventId) {
    Map<Long, Long> result = new java.util.HashMap<>();
    for (WaitlistEntry entry : entries.findByEventIdOrderByCreatedAtAscIdAsc(eventId)) {
      if (entry.getStatus() == WaitlistStatus.WAITING) {
        result.merge(entry.getTier().getId(), 1L, Long::sum);
      }
    }
    return result;
  }

  /** Whether the order holds tickets offered to someone on the waitlist. */
  @Transactional(readOnly = true)
  public boolean isOffer(long orderId) {
    return entries.findByOfferOrderId(orderId).isPresent();
  }

  @Transactional(readOnly = true)
  public long waitingCount(long tierId) {
    return entries.countByTierIdAndStatus(tierId, WaitlistStatus.WAITING);
  }
}

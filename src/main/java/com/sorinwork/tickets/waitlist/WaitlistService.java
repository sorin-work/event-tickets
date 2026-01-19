package com.sorinwork.tickets.waitlist;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.notify.Notifier;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.TierAvailability;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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
  private final Notifier notifier;
  private final AppProperties properties;
  private final Clock clock;

  public WaitlistService(
      WaitlistRepository entries,
      TicketTierRepository tiers,
      EventRepository events,
      AvailabilityService availability,
      CheckoutService checkout,
      Notifier notifier,
      AppProperties properties,
      Clock clock) {
    this.entries = entries;
    this.tiers = tiers;
    this.events = events;
    this.availability = availability;
    this.checkout = checkout;
    this.notifier = notifier;
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
    return entries.save(
        new WaitlistEntry(tier, form.getName().trim(), email, form.getQuantity(), clock.instant()));
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
          entries.findByTierIdAndStatusOrderByCreatedAtDesc(tier.getId(), WaitlistStatus.WAITING)) {
        if (entry.getQuantity() > remaining) {
          break;
        }
        Order hold =
            checkout.holdForWaitlist(
                event, tier, entry.getQuantity(), entry.getName(), entry.getEmail(), until);
        entry.offer(hold, now);
        sendOffer(entry, hold);
        remaining -= entry.getQuantity();
        offered++;
      }
    }
    return offered;
  }

  private void sendOffer(WaitlistEntry entry, Order hold) {
    Event event = hold.getEvent();
    String body =
        "Hi "
            + entry.getName()
            + ",\n\nGood news: "
            + entry.getQuantity()
            + " × "
            + entry.getTier().getName()
            + " for "
            + event.getTitle()
            + " became available and we are holding them for you until "
            + hold.getHoldExpiresAt().atZone(event.zone()).toLocalDateTime().withSecond(0)
            + " ("
            + event.getTimeZone()
            + ").\n\nComplete your order here:\n"
            + properties.url("/checkout/" + hold.getAccessToken())
            + "\n\nIf you no longer want them, just ignore this email and they will go to the"
            + " next person on the list.\n";
    notifier.send(entry.getEmail(), "Tickets available: " + event.getTitle(), body, event.getId());
  }

  @Transactional(readOnly = true)
  public long waitingCount(long tierId) {
    return entries.countByTierIdAndStatus(tierId, WaitlistStatus.WAITING);
  }
}

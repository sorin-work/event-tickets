package com.sorinwork.tickets.order;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.common.Tokens;
import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.notify.Emails;
import com.sorinwork.tickets.promo.DiscountCalculator;
import com.sorinwork.tickets.promo.PromoCode;
import com.sorinwork.tickets.promo.PromoCodeRepository;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.ticket.TicketRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Turns a ticket selection into an order that holds the seats while the buyer checks out. */
@Service
@Transactional
public class CheckoutService {

  private final EventRepository events;
  private final TicketTierRepository tiers;
  private final OrderRepository orders;
  private final TicketRepository tickets;
  private final PromoCodeRepository promoCodes;
  private final AvailabilityService availability;
  private final AppProperties properties;
  private final Emails emails;
  private final ApplicationEventPublisher publisher;
  private final Clock clock;

  public CheckoutService(
      EventRepository events,
      TicketTierRepository tiers,
      OrderRepository orders,
      TicketRepository tickets,
      PromoCodeRepository promoCodes,
      AvailabilityService availability,
      AppProperties properties,
      Emails emails,
      ApplicationEventPublisher publisher,
      Clock clock) {
    this.events = events;
    this.tiers = tiers;
    this.orders = orders;
    this.tickets = tickets;
    this.promoCodes = promoCodes;
    this.availability = availability;
    this.properties = properties;
    this.emails = emails;
    this.publisher = publisher;
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
    if (!event.startsAtInstant().isAfter(now)) {
      throw new BusinessException("Ticket sales have closed: the event has started.");
    }
    Map<Long, TierAvailability> available = availability.forEvent(eventId);
    Order order = newOrder(event, now, now.plus(properties.checkout().holdDuration()));

    for (var entry : quantities.entrySet()) {
      int quantity = entry.getValue() == null ? 0 : entry.getValue();
      if (quantity < 0) {
        throw new BusinessException("Ticket quantities cannot be negative.");
      }
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

  /** Applies a promo code to an open checkout, replacing any code applied before. */
  public Order applyPromoCode(String token, String rawCode) {
    Order order = getOpenOrder(token);
    String code = PromoCode.normalize(rawCode);
    if (code == null || code.isEmpty()) {
      throw new BusinessException("Enter a promo code.");
    }
    PromoCode promo =
        promoCodes
            .findByEventIdAndCode(order.getEvent().getId(), code)
            .filter(PromoCode::isActive)
            .orElseThrow(() -> new BusinessException("That promo code is not valid."));
    LocalDateTime eventNow = LocalDateTime.now(clock.withZone(order.getEvent().zone()));
    if (promo.getValidUntil() != null && eventNow.isAfter(promo.getValidUntil())) {
      throw new BusinessException("That promo code has expired.");
    }
    if (promo.getId().equals(order.getPromoCode() == null ? null : order.getPromoCode().getId())) {
      return order;
    }
    if (promo.isUsedUp()) {
      throw new BusinessException("That promo code has been used up.");
    }
    releasePromoCode(order);
    promo.setUsedCount(promo.getUsedCount() + 1);
    order.setPromoCode(promo);
    order.setDiscountCents(DiscountCalculator.discount(promo, order.getSubtotalCents()));
    return order;
  }

  public Order removePromoCode(String token) {
    Order order = getOpenOrder(token);
    releasePromoCode(order);
    return order;
  }

  private void releasePromoCode(Order order) {
    PromoCode current = order.getPromoCode();
    if (current != null) {
      current.setUsedCount(Math.max(0, current.getUsedCount() - 1));
      order.setPromoCode(null);
      order.setDiscountCents(0);
    }
  }

  private Order getOpenOrder(String token) {
    Order order = getByToken(token);
    if (!order.isHoldActive(clock.instant())) {
      throw new BusinessException(
          "Your reservation has expired. Please choose your tickets again.");
    }
    return order;
  }

  /**
   * Completes checkout with the buyer's details. Payment is simulated: the order is marked paid and
   * its tickets are issued straight away.
   */
  public Order complete(String token, CheckoutForm form) {
    Order order = getByToken(token);
    Instant now = clock.instant();
    if (order.getStatus() == OrderStatus.PAID) {
      throw new BusinessException("This order has already been paid.");
    }
    if (!order.isHoldActive(now)) {
      throw new BusinessException(
          "Your reservation has expired. Please choose your tickets again.");
    }
    order.setBuyerName(form.getName().trim());
    order.setBuyerEmail(form.getEmail().trim().toLowerCase(Locale.ROOT));
    order.setStatus(OrderStatus.PAID);
    order.setPaidAt(now);
    issueTickets(order, form.getAttendeeNames(), now);
    emails.orderConfirmation(order);
    publisher.publishEvent(new OrderEvents.OrderPaid(order.getId()));
    return order;
  }

  @Transactional(readOnly = true)
  public List<Ticket> ticketsOf(Order order) {
    return tickets.findByOrderIdOrderById(order.getId());
  }

  /**
   * One ticket per seat. {@code names} are optional attendee names in seat order; seats without a
   * name are issued to the buyer.
   */
  private void issueTickets(Order order, List<String> names, Instant now) {
    int seat = 0;
    for (OrderItem item : order.getItems()) {
      for (int i = 0; i < item.getQuantity(); i++, seat++) {
        String name = seat < names.size() ? names.get(seat) : null;
        tickets.save(
            new Ticket(
                order,
                item.getTier(),
                Tokens.ticketCode(),
                name == null || name.isBlank() ? order.getBuyerName() : name.trim(),
                order.getBuyerEmail(),
                now));
      }
    }
  }

  /**
   * Reserves tickets for someone from the waitlist. Unlike {@link #start} the caller has already
   * locked the event and checked availability, and the hold lasts as long as the offer.
   */
  public Order holdForWaitlist(
      Event event, TicketTier tier, int quantity, String name, String email, Instant until) {
    Order order = newOrder(event, clock.instant(), until);
    order.setBuyerName(name);
    order.setBuyerEmail(email);
    order.addItem(new OrderItem(order, tier, quantity));
    return orders.save(order);
  }

  private static Order newOrder(Event event, Instant now, Instant holdUntil) {
    return new Order(event, Tokens.orderReference(), Tokens.accessToken(), now, holdUntil);
  }

  @Transactional(readOnly = true)
  public Order getByToken(String token) {
    return orders
        .findWithItemsByAccessToken(token)
        .orElseThrow(() -> new NotFoundException("Order"));
  }

  private void checkOnSale(TicketTier tier) {
    if (tier.isPaused()) {
      throw new BusinessException(tier.getName() + " is not on sale right now.");
    }
    LocalDateTime now = LocalDateTime.now(clock);
    if (tier.getSalesStart() != null && now.isBefore(tier.getSalesStart())) {
      throw new BusinessException(tier.getName() + " is not on sale yet.");
    }
    if (tier.getSalesEnd() != null && !now.isBefore(tier.getSalesEnd())) {
      throw new BusinessException("Sales for " + tier.getName() + " have ended.");
    }
  }
}

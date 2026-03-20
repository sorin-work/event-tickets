package com.sorinwork.tickets.order;

import com.sorinwork.tickets.notify.Emails;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Lets buyers who lost their email get their ticket links again. */
@Service
@Transactional(readOnly = true)
public class TicketLookupService {

  private final OrderRepository orders;
  private final Emails emails;
  private final Clock clock;

  public TicketLookupService(OrderRepository orders, Emails emails, Clock clock) {
    this.orders = orders;
    this.emails = emails;
    this.clock = clock;
  }

  /**
   * Emails the links of the buyer's upcoming orders to that address. The links only ever go to the
   * inbox, never to the page, so typing someone else's email reveals nothing.
   *
   * @return whether an email was sent
   */
  public boolean sendLinks(String email) {
    String normalized = email.trim().toLowerCase(Locale.ROOT);
    List<Order> upcoming =
        orders.findUpcomingForBuyer(
            normalized, OrderStatus.PAID, LocalDateTime.now(clock).minusDays(1));
    if (upcoming.isEmpty()) {
      return false;
    }
    emails.ticketLinks(normalized, upcoming);
    return true;
  }
}

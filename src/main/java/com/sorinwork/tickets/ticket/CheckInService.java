package com.sorinwork.tickets.ticket;

import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.order.OrderStatus;
import java.time.Clock;
import java.util.Locale;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class CheckInService {

  private final TicketRepository tickets;
  private final Clock clock;

  public CheckInService(TicketRepository tickets, Clock clock) {
    this.tickets = tickets;
    this.clock = clock;
  }

  /**
   * Checks a ticket in at the door of {@code eventId}. The code may be typed by hand, so spaces,
   * dashes and lower case are accepted.
   */
  public CheckInResult checkIn(long eventId, String rawCode, String staff) {
    Optional<Ticket> found = tickets.findWithDetailsByCode(normalize(rawCode));
    if (found.isEmpty()) {
      return CheckInResult.of(CheckInResult.Status.NOT_FOUND, null);
    }
    Ticket ticket = found.get();
    if (!ticket.getEvent().getId().equals(eventId)) {
      return CheckInResult.of(CheckInResult.Status.WRONG_EVENT, ticket);
    }
    if (ticket.getOrder().getStatus() != OrderStatus.PAID) {
      return CheckInResult.of(CheckInResult.Status.CANCELLED, ticket);
    }
    if (ticket.isCheckedIn()) {
      return CheckInResult.of(CheckInResult.Status.ALREADY_CHECKED_IN, ticket);
    }
    ticket.checkIn(clock.instant(), staff);
    return CheckInResult.of(CheckInResult.Status.CHECKED_IN, ticket);
  }

  /** Reverts a check-in made by mistake, e.g. a scan of the wrong person's phone. */
  public Ticket undo(long ticketId) {
    Ticket ticket = tickets.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket"));
    ticket.undoCheckIn();
    return ticket;
  }

  @Transactional(readOnly = true)
  public long checkedInCount(long eventId) {
    return tickets.countByEventIdAndCheckedInAtIsNotNull(eventId);
  }

  static String normalize(String code) {
    return code == null ? "" : code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
  }
}

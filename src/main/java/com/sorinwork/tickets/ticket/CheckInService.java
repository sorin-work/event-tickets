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
    // Another door may have scanned the same code a moment ago, or the order may have been
    // cancelled since it was loaded: the update only lets the ticket in if neither happened.
    int admitted = tickets.markCheckedIn(ticket.getId(), clock.instant(), staff, OrderStatus.PAID);
    Ticket current = tickets.findWithDetailsByCode(ticket.getCode()).orElseThrow();
    if (admitted == 1) {
      return CheckInResult.of(CheckInResult.Status.CHECKED_IN, current);
    }
    if (current.getOrder().getStatus() != OrderStatus.PAID) {
      return CheckInResult.of(CheckInResult.Status.CANCELLED, current);
    }
    return CheckInResult.of(CheckInResult.Status.ALREADY_CHECKED_IN, current);
  }

  /** Reverts a check-in made by mistake, e.g. a scan of the wrong person's phone. */
  public Ticket undo(long ticketId) {
    tickets.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket"));
    tickets.clearCheckIn(ticketId);
    return tickets.findById(ticketId).orElseThrow();
  }

  @Transactional(readOnly = true)
  public long checkedInCount(long eventId) {
    return tickets.countByEventIdAndCheckedInAtIsNotNullAndOrderStatus(eventId, OrderStatus.PAID);
  }

  /** The last few people let in, newest first, so door staff can see who just came in. */
  @Transactional(readOnly = true)
  public java.util.List<Ticket> recent(long eventId) {
    return tickets.findTop8ByEventIdAndCheckedInAtIsNotNullOrderByCheckedInAtDesc(eventId);
  }

  /** Checked-in and total valid tickets, for the counter at the door. */
  @Transactional(readOnly = true)
  public Progress progress(long eventId) {
    return new Progress(
        checkedInCount(eventId), tickets.countByEventIdAndOrderStatus(eventId, OrderStatus.PAID));
  }

  public record Progress(long checkedIn, long total) {

    public int percent() {
      return total == 0 ? 0 : (int) (checkedIn * 100 / total);
    }
  }

  static String normalize(String code) {
    return code == null ? "" : code.replaceAll("[\\s-]", "").toUpperCase(Locale.ROOT);
  }
}

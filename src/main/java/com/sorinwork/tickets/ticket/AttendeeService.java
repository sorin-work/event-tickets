package com.sorinwork.tickets.ticket;

import com.sorinwork.tickets.common.Csv;
import com.sorinwork.tickets.common.PageView;
import com.sorinwork.tickets.order.OrderStatus;
import java.io.Writer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttendeeService {

  public static final int PAGE_SIZE = 25;

  private final TicketRepository tickets;

  public AttendeeService(TicketRepository tickets) {
    this.tickets = tickets;
  }

  /**
   * Holders of valid tickets for the event, by name. {@code query} matches part of a name or email,
   * or a whole ticket code or order reference. {@code page} is 1-based.
   */
  public PageView<Ticket> attendees(long eventId, String query, int page) {
    String q = query == null ? "" : query.trim();
    String pattern = "%" + escapeLike(q.toLowerCase(Locale.ROOT)) + "%";
    PageRequest request = PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE);
    return PageView.of(tickets.findAttendees(eventId, OrderStatus.PAID, q, pattern, request));
  }

  /** Writes every valid ticket of the event as CSV, for door lists and mail merges. */
  public void exportCsv(long eventId, ZoneId zone, Writer out) {
    DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone);
    Csv csv = new Csv(out);
    csv.row("Name", "Email", "Tier", "Ticket code", "Order", "Checked in");
    for (Ticket ticket : tickets.findByEventIdAndOrderStatusOrderById(eventId, OrderStatus.PAID)) {
      csv.row(
          ticket.getAttendeeName(),
          ticket.getAttendeeEmail(),
          ticket.getTier().getName(),
          ticket.getCode(),
          ticket.getOrder().getReference(),
          ticket.isCheckedIn() ? time.format(ticket.getCheckedInAt()) : "");
    }
  }

  private static String escapeLike(String text) {
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }
}

package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.ticket.CheckInResult;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.web.Formatters;
import org.springframework.stereotype.Component;
import org.springframework.web.util.HtmlUtils;

/** The short message door staff see after each scan. */
@Component
public class CheckInMessages {

  private final Formatters fmt;

  public CheckInMessages(Formatters fmt) {
    this.fmt = fmt;
  }

  /**
   * HTML for the result box. Attendee names are typed by buyers, so everything that comes from the
   * database is escaped before it goes into the markup.
   */
  public String html(CheckInResult result, Event event) {
    Ticket ticket = result.ticket();
    return switch (result.status()) {
      case CHECKED_IN ->
          "<strong>"
              + escape(ticket.getAttendeeName())
              + "</strong> · "
              + escape(ticket.getTier().getName());
      case ALREADY_CHECKED_IN ->
          "<strong>"
              + escape(ticket.getAttendeeName())
              + "</strong> already checked in at "
              + fmt.instant(ticket.getCheckedInAt(), event)
              + (ticket.getCheckedInBy() == null ? "" : " by " + escape(ticket.getCheckedInBy()));
      case WRONG_EVENT ->
          "This ticket is for <strong>" + escape(ticket.getEvent().getTitle()) + "</strong>";
      case CANCELLED -> "The order for this ticket was cancelled";
      case NOT_FOUND -> "No ticket with this code";
    };
  }

  private static String escape(String text) {
    return HtmlUtils.htmlEscape(text, "UTF-8");
  }
}

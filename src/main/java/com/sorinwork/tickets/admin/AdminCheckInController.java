package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.ticket.CheckInResult;
import com.sorinwork.tickets.ticket.CheckInService;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.web.Formatters;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.HtmlUtils;

@Controller
@RequestMapping("/admin/events/{eventId}/check-in")
public class AdminCheckInController {

  private final EventService events;
  private final CheckInService checkIn;
  private final Formatters fmt;

  public AdminCheckInController(EventService events, CheckInService checkIn, Formatters fmt) {
    this.events = events;
    this.checkIn = checkIn;
    this.fmt = fmt;
  }

  @GetMapping
  public String page(@PathVariable long eventId, Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("progress", checkIn.progress(eventId));
    return "admin/checkin/page";
  }

  @GetMapping("/progress")
  public String progress(@PathVariable long eventId, Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("progress", checkIn.progress(eventId));
    return "admin/checkin/page :: progress";
  }

  @PostMapping
  public String scan(
      @PathVariable long eventId,
      @RequestParam(defaultValue = "") String code,
      @RequestHeader(value = "HX-Request", required = false) String htmx,
      Principal principal,
      Model model) {
    Event event = events.get(eventId);
    CheckInResult result = checkIn.checkIn(eventId, code, principal.getName());
    model.addAttribute("event", event);
    model.addAttribute("result", result);
    model.addAttribute("message", message(result, event));
    model.addAttribute("progress", checkIn.progress(eventId));
    model.addAttribute("oob", htmx != null);
    return htmx != null ? "admin/checkin/page :: result-and-progress" : "admin/checkin/page";
  }

  /**
   * HTML for the result box. Attendee names are typed by buyers, so everything that comes from the
   * database is escaped before it goes into the markup.
   */
  private String message(CheckInResult result, Event event) {
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
              + fmt.instant(ticket.getCheckedInAt(), event);
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

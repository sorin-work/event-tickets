package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.ticket.AttendeeService;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
public class AdminAttendeeController {

  private final EventService events;
  private final AttendeeService attendees;

  private final com.sorinwork.tickets.ticket.CheckInService checkIn;
  private final com.sorinwork.tickets.ticket.TicketRepository tickets;

  public AdminAttendeeController(
      EventService events,
      AttendeeService attendees,
      com.sorinwork.tickets.ticket.CheckInService checkIn,
      com.sorinwork.tickets.ticket.TicketRepository tickets) {
    this.events = events;
    this.attendees = attendees;
    this.checkIn = checkIn;
    this.tickets = tickets;
  }

  /** For people who lost their QR code: find them in the list and let them in by hand. */
  @org.springframework.web.bind.annotation.PostMapping(
      "/admin/events/{eventId}/attendees/{ticketId}/check-in")
  public String checkInByHand(
      @PathVariable long eventId,
      @PathVariable long ticketId,
      @RequestParam(defaultValue = "") String q,
      java.security.Principal principal,
      org.springframework.web.servlet.mvc.support.RedirectAttributes redirect) {
    var ticket =
        tickets
            .findById(ticketId)
            .orElseThrow(() -> new com.sorinwork.tickets.common.NotFoundException("Ticket"));
    var result = checkIn.checkIn(eventId, ticket.getCode(), principal.getName());
    redirect.addFlashAttribute(
        result.admitted() ? "flash" : "error",
        result.admitted()
            ? ticket.getAttendeeName() + " is checked in."
            : "Not checked in: " + result.status().name().toLowerCase().replace('_', ' ') + ".");
    redirect.addAttribute("q", q);
    return "redirect:/admin/events/" + eventId + "/attendees";
  }

  @GetMapping("/admin/events/{eventId}/attendees")
  public String list(
      @PathVariable long eventId,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "ALL") AttendeeService.Arrival arrival,
      @RequestParam(defaultValue = "1") int page,
      Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("q", q);
    model.addAttribute("arrival", arrival);
    model.addAttribute("attendees", attendees.attendees(eventId, q, arrival, page));
    return "admin/attendees/list";
  }

  @GetMapping("/admin/events/{eventId}/attendees.csv")
  public void exportCsv(@PathVariable long eventId, HttpServletResponse response)
      throws IOException {
    Event event = events.get(eventId);
    response.setContentType("text/csv;charset=UTF-8");
    response.setHeader(
        HttpHeaders.CONTENT_DISPOSITION,
        ContentDisposition.attachment()
            .filename(event.getSlug() + "-attendees.csv", StandardCharsets.UTF_8)
            .build()
            .toString());
    attendees.exportCsv(eventId, event.zone(), response.getWriter());
  }
}

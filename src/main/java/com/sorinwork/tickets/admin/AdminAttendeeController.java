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

  public AdminAttendeeController(EventService events, AttendeeService attendees) {
    this.events = events;
    this.attendees = attendees;
  }

  @GetMapping("/admin/events/{eventId}/attendees")
  public String list(
      @PathVariable long eventId,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "1") int page,
      Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("q", q);
    model.addAttribute("attendees", attendees.attendees(eventId, q, page));
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

package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.ticket.AttendeeService;
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
      @PathVariable long eventId, @RequestParam(defaultValue = "1") int page, Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("attendees", attendees.attendees(eventId, page));
    return "admin/attendees/list";
  }
}

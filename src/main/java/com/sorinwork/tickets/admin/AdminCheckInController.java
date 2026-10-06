package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.ticket.CheckInResult;
import com.sorinwork.tickets.ticket.CheckInService;
import java.security.Principal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/events/{eventId}/check-in")
public class AdminCheckInController {

  private final EventService events;
  private final CheckInService checkIn;
  private final CheckInMessages messages;

  public AdminCheckInController(
      EventService events, CheckInService checkIn, CheckInMessages messages) {
    this.events = events;
    this.checkIn = checkIn;
    this.messages = messages;
  }

  @GetMapping
  public String page(@PathVariable long eventId, Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("progress", checkIn.progress(eventId));
    model.addAttribute("recent", checkIn.recent(eventId));
    return "admin/checkin/page";
  }

  @GetMapping("/progress")
  public String progress(@PathVariable long eventId, Model model) {
    model.addAttribute("event", events.get(eventId));
    model.addAttribute("progress", checkIn.progress(eventId));
    model.addAttribute("recent", checkIn.recent(eventId));
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
    model.addAttribute("message", messages.html(result, event));
    model.addAttribute("progress", checkIn.progress(eventId));
    model.addAttribute("recent", checkIn.recent(eventId));
    model.addAttribute("oob", htmx != null);
    return htmx != null ? "admin/checkin/page :: result-and-progress" : "admin/checkin/page";
  }
}

package com.sorinwork.tickets.api;

import com.sorinwork.tickets.common.PageView;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.ticket.AttendeeService;
import com.sorinwork.tickets.ticket.CheckInResult;
import com.sorinwork.tickets.ticket.CheckInService;
import com.sorinwork.tickets.ticket.Ticket;
import java.security.Principal;
import java.time.Instant;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Endpoints for scanner apps used by door staff. */
@RestController
@RequestMapping("/api/v1/admin/events/{eventId}")
public class DoorApiController {

  private final EventService events;
  private final CheckInService checkIn;
  private final AttendeeService attendees;

  public DoorApiController(EventService events, CheckInService checkIn, AttendeeService attendees) {
    this.events = events;
    this.checkIn = checkIn;
    this.attendees = attendees;
  }

  public record CheckInRequest(String code) {}

  public record CheckInResponse(
      String status,
      boolean admitted,
      String attendee,
      String tier,
      Instant checkedInAt,
      long checkedIn,
      long total) {}

  public record Attendee(
      String code, String name, String email, String tier, String order, Instant checkedInAt) {

    static Attendee of(Ticket ticket) {
      return new Attendee(
          ticket.getCode(),
          ticket.getAttendeeName(),
          ticket.getAttendeeEmail(),
          ticket.getTier().getName(),
          ticket.getOrder().getReference(),
          ticket.getCheckedInAt());
    }
  }

  public record AttendeePage(List<Attendee> items, int page, int totalPages, long total) {}

  @PostMapping("/check-in")
  public CheckInResponse checkIn(
      @PathVariable long eventId, @RequestBody CheckInRequest request, Principal principal) {
    events.get(eventId);
    String staff = principal != null ? principal.getName() : "api";
    CheckInResult result = checkIn.checkIn(eventId, request.code(), staff);
    Ticket ticket = result.ticket();
    CheckInService.Progress progress = checkIn.progress(eventId);
    return new CheckInResponse(
        result.status().name(),
        result.admitted(),
        ticket == null ? null : ticket.getAttendeeName(),
        ticket == null ? null : ticket.getTier().getName(),
        ticket == null ? null : ticket.getCheckedInAt(),
        progress.checkedIn(),
        progress.total());
  }

  @GetMapping("/attendees")
  public AttendeePage attendees(
      @PathVariable long eventId,
      @RequestParam(defaultValue = "") String q,
      @RequestParam(defaultValue = "1") int page) {
    events.get(eventId);
    PageView<Ticket> view = attendees.attendees(eventId, q, page);
    return new AttendeePage(
        view.items().stream().map(Attendee::of).toList(),
        view.page(),
        view.totalPages(),
        view.total());
  }
}

package com.sorinwork.tickets.web;

import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.event.CalendarFile;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TierService;
import com.sorinwork.tickets.order.AvailabilityService;
import java.time.Clock;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class PublicEventController {

  private final EventService events;
  private final TierService tiers;
  private final AvailabilityService availability;
  private final AppProperties properties;
  private final Clock clock;

  public PublicEventController(
      EventService events,
      TierService tiers,
      AvailabilityService availability,
      AppProperties properties,
      Clock clock) {
    this.events = events;
    this.tiers = tiers;
    this.availability = availability;
    this.properties = properties;
    this.clock = clock;
  }

  @GetMapping(value = "/events/{slug}/calendar.ics", produces = "text/calendar")
  @ResponseBody
  public ResponseEntity<String> calendar(@PathVariable String slug) {
    Event event = events.getPublic(slug);
    String ics = CalendarFile.of(event, properties.url("/events/" + slug), clock.instant());
    return ResponseEntity.ok()
        .header(
            HttpHeaders.CONTENT_DISPOSITION,
            ContentDisposition.attachment().filename(slug + ".ics").build().toString())
        .contentType(MediaType.parseMediaType("text/calendar;charset=UTF-8"))
        .body(ics);
  }

  @GetMapping("/events/{slug}")
  public String show(@PathVariable String slug, Model model) {
    Event event = events.getPublic(slug);
    model.addAttribute("event", event);
    model.addAttribute("tiers", tiers.forEvent(event.getId()));
    model.addAttribute("availability", availability.forEvent(event.getId()));
    return "public/event";
  }
}

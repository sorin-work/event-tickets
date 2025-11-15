package com.sorinwork.tickets.web;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TierService;
import com.sorinwork.tickets.order.AvailabilityService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class PublicEventController {

  private final EventService events;
  private final TierService tiers;
  private final AvailabilityService availability;

  public PublicEventController(
      EventService events, TierService tiers, AvailabilityService availability) {
    this.events = events;
    this.tiers = tiers;
    this.availability = availability;
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

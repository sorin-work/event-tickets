package com.sorinwork.tickets.api;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TierService;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.order.TierAvailability;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Read-only JSON for embedding upcoming events on other sites. */
@RestController
@RequestMapping("/api/v1/events")
public class EventApiController {

  private final EventService events;
  private final TierService tiers;
  private final AvailabilityService availability;

  public EventApiController(
      EventService events, TierService tiers, AvailabilityService availability) {
    this.events = events;
    this.tiers = tiers;
    this.availability = availability;
  }

  public record EventSummary(
      String slug,
      String title,
      String venue,
      LocalDateTime startsAt,
      LocalDateTime endsAt,
      String timeZone,
      String status) {

    static EventSummary of(Event event) {
      return new EventSummary(
          event.getSlug(),
          event.getTitle(),
          event.getVenueName(),
          event.getStartsAt(),
          event.getEndsAt(),
          event.getTimeZone(),
          event.getStatus().name());
    }
  }

  public record TierInfo(
      long id,
      String name,
      String description,
      long priceCents,
      String currency,
      int remaining,
      boolean soldOut,
      int maxPerOrder,
      LocalDateTime salesStart,
      LocalDateTime salesEnd,
      boolean paused) {}

  public record EventDetails(
      EventSummary event, String description, String venueAddress, List<TierInfo> tiers) {}

  @GetMapping
  public List<EventSummary> upcoming() {
    return events.upcomingPublished().stream().map(EventSummary::of).toList();
  }

  @GetMapping("/{slug}")
  public EventDetails show(@PathVariable String slug) {
    Event event = events.getPublic(slug);
    Map<Long, TierAvailability> counts = availability.forEvent(event.getId());
    List<TierInfo> tierInfo =
        tiers.forEvent(event.getId()).stream()
            .map(tier -> tierInfo(tier, event, counts.get(tier.getId())))
            .toList();
    return new EventDetails(
        EventSummary.of(event), event.getDescription(), event.getVenueAddress(), tierInfo);
  }

  private static TierInfo tierInfo(TicketTier tier, Event event, TierAvailability count) {
    return new TierInfo(
        tier.getId(),
        tier.getName(),
        tier.getDescription(),
        tier.getPriceCents(),
        event.getCurrency(),
        count.remaining(),
        count.isSoldOut(),
        tier.getMaxPerOrder(),
        tier.getSalesStart(),
        tier.getSalesEnd(),
        tier.isPaused());
  }
}

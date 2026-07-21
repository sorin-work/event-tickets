package com.sorinwork.tickets.event;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.common.Slugs;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class EventService {

  private final EventRepository events;
  private final TicketTierRepository tiers;
  private final ApplicationEventPublisher publisher;
  private final Clock clock;

  public EventService(
      EventRepository events,
      TicketTierRepository tiers,
      ApplicationEventPublisher publisher,
      Clock clock) {
    this.events = events;
    this.tiers = tiers;
    this.publisher = publisher;
    this.clock = clock;
  }

  @Transactional(readOnly = true)
  public Event get(long id) {
    return events.findById(id).orElseThrow(() -> new NotFoundException("Event " + id));
  }

  /** Published or cancelled events by slug; drafts are not visible to the public. */
  @Transactional(readOnly = true)
  public Event getPublic(String slug) {
    return events
        .findBySlug(slug)
        .filter(e -> e.getStatus() != EventStatus.DRAFT)
        .orElseThrow(() -> new NotFoundException("Event " + slug));
  }

  @Transactional(readOnly = true)
  public List<Event> listAll() {
    return events.findAllByOrderByStartsAtDesc();
  }

  @Transactional(readOnly = true)
  public List<Event> upcomingPublished() {
    return events.findByStatusInAndStartsAtAfterOrderByStartsAt(
        List.of(EventStatus.PUBLISHED), LocalDateTime.now(clock).minusHours(6));
  }

  /** The 20 most recent published events that already took place. */
  @Transactional(readOnly = true)
  public List<Event> recentPast() {
    return events.findTop20ByStatusAndStartsAtBeforeOrderByStartsAtDesc(
        EventStatus.PUBLISHED, LocalDateTime.now(clock).minusHours(6));
  }

  /** Drafts and published events that have not started yet, soonest first. */
  @Transactional(readOnly = true)
  public List<Event> upcomingForAdmin() {
    return events.findByStatusInAndStartsAtAfterOrderByStartsAt(
        List.of(EventStatus.DRAFT, EventStatus.PUBLISHED), LocalDateTime.now(clock));
  }

  public Event create(EventForm form) {
    Event event = new Event(uniqueSlug(form.getTitle()), form.getTitle().trim(), clock.instant());
    apply(event, form);
    return events.save(event);
  }

  public Event update(long id, EventForm form) {
    Event event = get(id);
    event.setTitle(form.getTitle().trim());
    apply(event, form);
    event.setUpdatedAt(clock.instant());
    return event;
  }

  public Event publish(long id) {
    Event event = get(id);
    if (event.getStatus() == EventStatus.CANCELLED) {
      throw new BusinessException("A cancelled event cannot be published again.");
    }
    if (tiers.countByEventId(id) == 0) {
      throw new BusinessException("Add at least one ticket tier before publishing.");
    }
    return changeStatus(event, EventStatus.PUBLISHED);
  }

  public Event unpublish(long id) {
    Event event = get(id);
    if (event.getStatus() != EventStatus.PUBLISHED) {
      throw new BusinessException("Only published events can be moved back to draft.");
    }
    return changeStatus(event, EventStatus.DRAFT);
  }

  /**
   * Gives the event a different address, e.g. a shorter one for posters. Old links stop working, so
   * this is best done before sharing the page.
   */
  public Event changeSlug(long id, String requested) {
    Event event = get(id);
    String slug = requested == null ? "" : requested.trim().toLowerCase(java.util.Locale.ROOT);
    if (!slug.matches("[a-z0-9]+(-[a-z0-9]+)*") || slug.length() < 3 || slug.length() > 80) {
      throw new BusinessException(
          "Use 3-80 lower case letters, digits and single dashes, e.g. jazz-night-2026.");
    }
    if (slug.equals(event.getSlug())) {
      return event;
    }
    if (events.existsBySlug(slug)) {
      throw new BusinessException("Another event already uses /events/" + slug + ".");
    }
    event.setSlug(slug);
    event.setUpdatedAt(clock.instant());
    return event;
  }

  /** Deletes a draft that never sold anything, e.g. one created by mistake. */
  public void delete(long id) {
    Event event = get(id);
    if (event.getStatus() != EventStatus.DRAFT) {
      throw new BusinessException("Only draft events can be deleted. Cancel it instead.");
    }
    if (events.countOrders(id) > 0) {
      throw new BusinessException("This event has orders and cannot be deleted.");
    }
    events.delete(event);
  }

  /** Cancels the event; its orders are cancelled and buyers emailed as well. */
  public Event cancel(long id) {
    Event event = get(id);
    if (event.getStatus() == EventStatus.CANCELLED) {
      return event;
    }
    changeStatus(event, EventStatus.CANCELLED);
    events.flush();
    publisher.publishEvent(new EventCancelled(id));
    return event;
  }

  private Event changeStatus(Event event, EventStatus status) {
    event.setStatus(status);
    event.setUpdatedAt(clock.instant());
    return event;
  }

  private void apply(Event event, EventForm form) {
    event.setDescription(blankToNull(form.getDescription()));
    event.setVenueName(form.getVenueName().trim());
    event.setVenueAddress(blankToNull(form.getVenueAddress()));
    event.setStartsAt(form.getStartsAt());
    event.setEndsAt(form.getEndsAt());
    event.setTimeZone(form.getTimeZone());
    event.setCurrency(form.getCurrency());
    event.setCapacity(form.getCapacity());
  }

  String uniqueSlug(String title) {
    String base = Slugs.slugify(title);
    String slug = base;
    int suffix = 2;
    while (events.existsBySlug(slug)) {
      slug = base + "-" + suffix++;
    }
    return slug;
  }

  private static String blankToNull(String value) {
    return value == null || value.isBlank() ? null : value.trim();
  }
}

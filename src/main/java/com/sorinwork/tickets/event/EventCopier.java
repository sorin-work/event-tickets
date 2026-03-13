package com.sorinwork.tickets.event;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Copies an event and its ticket tiers into a new draft, for series like monthly meetups. */
@Service
@Transactional
public class EventCopier {

  private final EventService events;
  private final TierService tiers;

  public EventCopier(EventService events, TierService tiers) {
    this.events = events;
    this.tiers = tiers;
  }

  public Event duplicate(long eventId) {
    Event source = events.get(eventId);
    EventForm form = EventForm.from(source);
    form.setTitle(copyTitle(source.getTitle()));
    Event copy = events.create(form);
    for (TicketTier tier : tiers.forEvent(eventId)) {
      TierForm tierForm = TierForm.from(tier);
      // Sales windows belong to the old date; the organiser sets new ones.
      tierForm.setSalesStart(null);
      tierForm.setSalesEnd(null);
      tiers.create(copy.getId(), tierForm);
    }
    return copy;
  }

  static String copyTitle(String title) {
    String copy = "Copy of " + title;
    return copy.length() > 160 ? copy.substring(0, 160) : copy;
  }
}

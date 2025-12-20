package com.sorinwork.tickets.support;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventForm;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.event.TierForm;
import com.sorinwork.tickets.event.TierService;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;

/** Builds events and tiers for integration tests through the real services. */
@Component
public class TestData {

  private final EventService events;
  private final TierService tiers;
  private final TicketTierRepository tierRepository;

  public TestData(EventService events, TierService tiers, TicketTierRepository tierRepository) {
    this.events = events;
    this.tiers = tiers;
    this.tierRepository = tierRepository;
  }

  public static EventForm eventForm(String title) {
    EventForm form = new EventForm();
    form.setTitle(title);
    form.setVenueName("The Attic");
    form.setStartsAt(LocalDateTime.of(2030, 3, 14, 19, 30));
    form.setTimeZone("Europe/Bucharest");
    form.setCurrency("EUR");
    return form;
  }

  public static TierForm tierForm(String name, String price, int quantity) {
    TierForm form = new TierForm();
    form.setName(name);
    form.setPrice(price);
    form.setQuantity(quantity);
    form.setMaxPerOrder(10);
    return form;
  }

  public Event event(String title) {
    return events.create(eventForm(title));
  }

  public TicketTier tier(Event event, String name, String price, int quantity) {
    return tiers.create(event.getId(), tierForm(name, price, quantity));
  }

  public TicketTier saveTier(TicketTier tier) {
    return tierRepository.save(tier);
  }
}

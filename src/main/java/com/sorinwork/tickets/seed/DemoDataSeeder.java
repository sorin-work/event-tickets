package com.sorinwork.tickets.seed;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventForm;
import com.sorinwork.tickets.event.EventRepository;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TierForm;
import com.sorinwork.tickets.event.TierService;
import com.sorinwork.tickets.order.CheckoutForm;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.promo.DiscountType;
import com.sorinwork.tickets.promo.PromoCodeService;
import com.sorinwork.tickets.promo.PromoForm;
import com.sorinwork.tickets.ticket.CheckInService;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.waitlist.WaitlistForm;
import com.sorinwork.tickets.waitlist.WaitlistService;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fills an empty database with a few events, orders, promo codes and a waitlist so the app can be
 * tried out straight away. Enabled with {@code SEED_DEMO=true}.
 */
@Component
@ConditionalOnProperty(name = "app.seed-demo", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

  private static final List<String> FIRST_NAMES =
      List.of(
          "Ana",
          "Bogdan",
          "Cristina",
          "Dan",
          "Elena",
          "Florin",
          "Gabriela",
          "Horia",
          "Ioana",
          "Mihai",
          "Andrei",
          "Maria",
          "Radu",
          "Sofia",
          "Tudor",
          "Irina",
          "Vlad",
          "Alexandra");
  private static final List<String> LAST_NAMES =
      List.of(
          "Popescu",
          "Ionescu",
          "Stan",
          "Dumitru",
          "Marin",
          "Constantin",
          "Georgescu",
          "Matei",
          "Rusu",
          "Lazăr",
          "Munteanu",
          "Ciobanu");

  private final EventRepository eventRepository;
  private final EventService events;
  private final TierService tiers;
  private final PromoCodeService promos;
  private final CheckoutService checkout;
  private final CheckInService checkIn;
  private final WaitlistService waitlist;
  private final Clock clock;
  private final Random random = new Random(4411);

  public DemoDataSeeder(
      EventRepository eventRepository,
      EventService events,
      TierService tiers,
      PromoCodeService promos,
      CheckoutService checkout,
      CheckInService checkIn,
      WaitlistService waitlist,
      Clock clock) {
    this.eventRepository = eventRepository;
    this.events = events;
    this.tiers = tiers;
    this.promos = promos;
    this.checkout = checkout;
    this.checkIn = checkIn;
    this.waitlist = waitlist;
    this.clock = clock;
  }

  @Override
  @Transactional
  public void run(ApplicationArguments args) {
    if (eventRepository.count() > 0) {
      log.info("Database already has events, skipping demo data");
      return;
    }
    LocalDate today = LocalDate.now(clock);

    Event jazz =
        event(
            "Summer Jazz Night",
            "The Attic",
            "Str. Lipscani 12, Bucharest",
            today.plusDays(24).atTime(20, 0),
            "RON",
            "Two bands, three sets and a late jam session.\n"
                + "Doors open at 19:30. Table seats include a welcome drink.");
    TicketTier earlyBird = tier(jazz, "Early bird", "45", 30, 4);
    TicketTier standing = tier(jazz, "Standing", "60", 120, 6);
    TicketTier table = tier(jazz, "Table seat", "95", 24, 4);
    events.publish(jazz.getId());
    promo(jazz, "FRIENDS10", DiscountType.PERCENT, "10", null);
    promo(jazz, "BAND", DiscountType.FIXED, "20", 10);
    sell(earlyBird, 30, 20);
    sell(standing, 41, 18);
    sell(table, 15, 12);
    TierForm earlyBirdForm = TierForm.from(earlyBird);
    earlyBirdForm.setSalesEnd(today.minusDays(3).atTime(23, 59));
    tiers.update(jazz.getId(), earlyBird.getId(), earlyBirdForm);

    Event meetup =
        event(
            "Rust & Coffee Meetup #12",
            "Hub Space",
            "Bd. Unirii 21, Cluj-Napoca",
            today.plusDays(9).atTime(18, 30),
            "EUR",
            "Lightning talks about Rust in production, followed by coffee and chats.\n"
                + "Free, but please register so we know how much coffee to make.");
    TicketTier attendee = tier(meetup, "Attendee", "0", 60, 2);
    TicketTier supporter = tier(meetup, "Supporter", "10", 20, 2);
    events.publish(meetup.getId());
    sell(attendee, 38, 10);
    sell(supporter, 6, 10);

    Event film =
        event(
            "Indie Film Club: Stalker",
            "Cinema Elvire",
            "Str. Popa Nan 15, Bucharest",
            today.plusDays(5).atTime(19, 0),
            "RON",
            "Tarkovsky's Stalker on 35mm, introduced by our guest critic.");
    TicketTier seat = tier(film, "Seat", "30", 40, 4);
    events.publish(film.getId());
    sell(seat, 40, 14);
    Event filmNow = events.get(film.getId());
    for (int i = 0; i < 4; i++) {
      waitlist.join(
          filmNow, new WaitlistForm(seat.getId(), name(), email(i, "waitlist"), 1 + i % 2));
    }

    Event workshop =
        event(
            "Pottery Workshop for Beginners",
            "Clay Studio",
            null,
            today.plusDays(40).atTime(11, 0),
            "RON",
            "Three hours at the wheel. All materials included.");
    tier(workshop, "Place", "180", 8, 2);

    Event past =
        event(
            "Autumn Poetry Slam",
            "The Attic",
            "Str. Lipscani 12, Bucharest",
            today.plusDays(1).atTime(19, 30),
            "RON",
            "Open mic poetry with a jury of three. Sign up at the door to perform.");
    TicketTier entry = tier(past, "Entry", "25", 80, 4);
    events.publish(past.getId());
    List<Ticket> pastTickets = sell(entry, 64, 30);
    // Sales close once an event starts, so sell first and then move it into the past.
    EventForm pastForm = EventForm.from(events.get(past.getId()));
    pastForm.setStartsAt(today.minusDays(12).atTime(19, 30));
    pastForm.setEndsAt(today.minusDays(12).atTime(22, 30));
    events.update(past.getId(), pastForm);
    for (Ticket ticket : pastTickets.subList(0, 52)) {
      checkIn.checkIn(past.getId(), ticket.getCode(), "door@example.com");
    }

    log.info("Created demo events, orders and a waitlist");
  }

  private Event event(
      String title,
      String venue,
      String address,
      LocalDateTime startsAt,
      String currency,
      String description) {
    EventForm form = new EventForm();
    form.setTitle(title);
    form.setVenueName(venue);
    form.setVenueAddress(address);
    form.setStartsAt(startsAt);
    form.setEndsAt(startsAt.plusHours(3));
    form.setTimeZone("Europe/Bucharest");
    form.setCurrency(currency);
    form.setDescription(description);
    return events.create(form);
  }

  private TicketTier tier(Event event, String name, String price, int quantity, int maxPerOrder) {
    TierForm form = new TierForm();
    form.setName(name);
    form.setPrice(price);
    form.setQuantity(quantity);
    form.setMaxPerOrder(maxPerOrder);
    return tiers.create(event.getId(), form);
  }

  private void promo(Event event, String code, DiscountType type, String amount, Integer maxUses) {
    PromoForm form = new PromoForm();
    form.setCode(code);
    form.setDiscountType(type);
    form.setAmount(amount);
    form.setMaxUses(maxUses);
    promos.create(event.getId(), form);
  }

  /** Sells {@code count} tickets of the tier in small orders paid over the last days. */
  private List<Ticket> sell(TicketTier tier, int count, int overDays) {
    List<Ticket> sold = new ArrayList<>();
    int left = count;
    int n = 0;
    while (left > 0) {
      int quantity = Math.min(left, 1 + random.nextInt(Math.min(3, tier.getMaxPerOrder())));
      Order order = checkout.start(tier.getEvent().getId(), Map.of(tier.getId(), quantity));
      String buyer = name();
      Order paid =
          checkout.complete(order.getAccessToken(), new CheckoutForm(buyer, email(n++, buyer)));
      paid.setPaidAt(
          clock
              .instant()
              .minus(Duration.ofDays(random.nextInt(overDays) + 1))
              .minus(Duration.ofMinutes(random.nextInt(600))));
      sold.addAll(checkout.ticketsOf(paid));
      left -= quantity;
    }
    return sold;
  }

  private String name() {
    return FIRST_NAMES.get(random.nextInt(FIRST_NAMES.size()))
        + " "
        + LAST_NAMES.get(random.nextInt(LAST_NAMES.size()));
  }

  private String email(int n, String seed) {
    String local =
        java.text.Normalizer.normalize(seed, java.text.Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(java.util.Locale.ROOT)
            .replaceAll("[^a-z]+", ".");
    return local + "." + n + "@example.com";
  }
}

package com.sorinwork.tickets.web;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class FormattersTest {

  private final Formatters fmt = new Formatters();

  private static Event event(LocalDateTime start, LocalDateTime end) {
    Event event = new Event("x", "X", Instant.EPOCH);
    event.setStartsAt(start);
    event.setEndsAt(end);
    event.setTimeZone("Europe/Bucharest");
    event.setCurrency("EUR");
    return event;
  }

  @Test
  void freeTicketsShowAsFree() {
    assertThat(fmt.price(0, "EUR")).isEqualTo("Free");
    assertThat(fmt.price(1500, "EUR")).isEqualTo("€15.00");
  }

  @Test
  void sameDayEventsShowOnlyTheEndTime() {
    Event event =
        event(LocalDateTime.of(2030, 3, 14, 19, 30), LocalDateTime.of(2030, 3, 14, 23, 0));

    assertThat(fmt.when(event)).isEqualTo("Thu 14 Mar 2030, 19:30 – 23:00");
  }

  @Test
  void multiDayEventsShowBothDates() {
    Event event = event(LocalDateTime.of(2030, 3, 14, 19, 30), LocalDateTime.of(2030, 3, 16, 2, 0));

    assertThat(fmt.when(event)).isEqualTo("Thu 14 Mar 2030, 19:30 – Sat 16 Mar 2030, 02:00");
  }

  @Test
  void eventsWithoutAnEnd() {
    assertThat(fmt.when(event(LocalDateTime.of(2030, 3, 14, 19, 30), null)))
        .isEqualTo("Thu 14 Mar 2030, 19:30");
  }

  @Test
  void instantsUseTheEventTimeZone() {
    Event event = event(LocalDateTime.of(2030, 7, 1, 20, 0), null);

    assertThat(fmt.instant(Instant.parse("2030-07-01T17:05:00Z"), event))
        .isEqualTo("Mon 1 Jul 2030, 20:05");
    assertThat(fmt.instant(null, event)).isEmpty();
  }
}

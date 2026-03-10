package com.sorinwork.tickets.event;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;

class CalendarFileTest {

  private static Event event() {
    Event event = new Event("jazz", "Jazz, Blues & More", Instant.EPOCH);
    event.setVenueName("The Attic");
    event.setVenueAddress("Str. Lipscani 12; Bucharest");
    event.setStartsAt(LocalDateTime.of(2030, 7, 1, 20, 0));
    event.setTimeZone("Europe/Bucharest");
    event.setDescription("Line one\nLine two");
    return event;
  }

  @Test
  void convertsTheStartToUtc() {
    String ics = CalendarFile.of(event(), "https://t.example/events/jazz", Instant.EPOCH);

    assertThat(ics).contains("DTSTART:20300701T170000Z\r\n").contains("DTEND:20300701T190000Z\r\n");
  }

  @Test
  void escapesText() {
    String ics = CalendarFile.of(event(), "https://t.example/events/jazz", Instant.EPOCH);

    assertThat(ics)
        .contains("SUMMARY:Jazz\\, Blues & More\r\n")
        .contains("LOCATION:The Attic\\, Str. Lipscani 12\\; Bucharest\r\n")
        .contains("DESCRIPTION:Line one\\nLine two\r\n");
  }

  @Test
  void foldsLongLines() {
    Event event = event();
    event.setDescription("x".repeat(200));

    String ics = CalendarFile.of(event, "https://t.example/events/jazz", Instant.EPOCH);

    assertThat(ics.lines()).allSatisfy(line -> assertThat(line.length()).isLessThanOrEqualTo(75));
    assertThat(ics).startsWith("BEGIN:VCALENDAR\r\n").endsWith("END:VCALENDAR\r\n");
  }
}

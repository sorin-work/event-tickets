package com.sorinwork.tickets.event;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

/** Builds an iCalendar (.ics) file so buyers can add an event to their calendar. */
public final class CalendarFile {

  private static final DateTimeFormatter UTC =
      DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'").withZone(ZoneOffset.UTC);

  private CalendarFile() {}

  public static String of(Event event, String url, Instant now) {
    Instant start = event.startsAtInstant();
    Instant end =
        event.getEndsAt() != null
            ? event.getEndsAt().atZone(event.zone()).toInstant()
            : start.plus(Duration.ofHours(2));
    String location =
        event.getVenueAddress() == null
            ? event.getVenueName()
            : event.getVenueName() + ", " + event.getVenueAddress();
    StringBuilder ics = new StringBuilder();
    line(ics, "BEGIN:VCALENDAR");
    line(ics, "VERSION:2.0");
    line(ics, "PRODID:-//event-tickets//EN");
    line(ics, "CALSCALE:GREGORIAN");
    line(ics, "BEGIN:VEVENT");
    line(ics, "UID:event-" + event.getId() + "@event-tickets");
    line(ics, "DTSTAMP:" + UTC.format(now));
    line(ics, "DTSTART:" + UTC.format(start));
    line(ics, "DTEND:" + UTC.format(end));
    line(ics, "SUMMARY:" + escape(event.getTitle()));
    line(ics, "LOCATION:" + escape(location));
    if (event.getDescription() != null) {
      line(ics, "DESCRIPTION:" + escape(event.getDescription()));
    }
    line(ics, "URL:" + url);
    line(ics, "END:VEVENT");
    line(ics, "END:VCALENDAR");
    return ics.toString();
  }

  static String escape(String text) {
    return text.replace("\\", "\\\\")
        .replace(";", "\\;")
        .replace(",", "\\,")
        .replace("\r\n", "\\n")
        .replace("\n", "\\n");
  }

  /** Content lines end in CRLF and are folded at 75 characters. */
  private static void line(StringBuilder ics, String content) {
    String rest = content;
    boolean first = true;
    while (rest.length() > (first ? 75 : 74)) {
      int cut = first ? 75 : 74;
      ics.append(first ? "" : " ").append(rest, 0, cut).append("\r\n");
      rest = rest.substring(cut);
      first = false;
    }
    ics.append(first ? "" : " ").append(rest).append("\r\n");
  }
}

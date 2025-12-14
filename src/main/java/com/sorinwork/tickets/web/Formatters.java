package com.sorinwork.tickets.web;

import com.sorinwork.tickets.common.Money;
import com.sorinwork.tickets.event.Event;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.stereotype.Component;

/** Helpers for templates, available as {@code ${@fmt.money(...)}}. */
@Component("fmt")
public class Formatters {

  private static final DateTimeFormatter DATE_TIME =
      DateTimeFormatter.ofPattern("EEE d MMM yyyy, HH:mm", Locale.ENGLISH);
  private static final DateTimeFormatter TIME =
      DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);

  public String money(long cents, String currency) {
    return Money.format(cents, currency);
  }

  public String price(long cents, String currency) {
    return cents == 0 ? "Free" : Money.format(cents, currency);
  }

  public String dateTime(LocalDateTime value) {
    return value == null ? "" : DATE_TIME.format(value);
  }

  /** E.g. "Fri 14 Mar 2030, 19:30 – 23:00". */
  public String when(Event event) {
    String start = DATE_TIME.format(event.getStartsAt());
    if (event.getEndsAt() == null) {
      return start;
    }
    if (event.getEndsAt().toLocalDate().equals(event.getStartsAt().toLocalDate())) {
      return start + " – " + TIME.format(event.getEndsAt());
    }
    return start + " – " + DATE_TIME.format(event.getEndsAt());
  }

  /** An instant shown as wall-clock time in the event's zone. */
  public String instant(Instant value, Event event) {
    return value == null ? "" : DATE_TIME.format(value.atZone(ZoneId.of(event.getTimeZone())));
  }
}

package com.sorinwork.tickets.order;

import com.sorinwork.tickets.notify.Emails;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Emails buyers a reminder with their ticket link about a day before the event. */
@Service
public class ReminderService {

  static final Duration AHEAD = Duration.ofHours(24);

  private static final Logger log = LoggerFactory.getLogger(ReminderService.class);

  private final OrderRepository orders;
  private final Emails emails;
  private final Clock clock;

  public ReminderService(OrderRepository orders, Emails emails, Clock clock) {
    this.orders = orders;
    this.emails = emails;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${app.reminders.interval:15m}", initialDelay = 60_000)
  public void run() {
    int sent = sendDueReminders();
    if (sent > 0) {
      log.info("Sent {} event reminder(s)", sent);
    }
  }

  @Transactional
  public int sendDueReminders() {
    Instant now = clock.instant();
    Instant until = now.plus(AHEAD);
    // Start times are local to each event; widen the window by the largest UTC offsets
    // and check the exact instant per event below.
    LocalDateTime from = LocalDateTime.ofInstant(now, ZoneOffset.UTC).minusHours(14);
    LocalDateTime to = LocalDateTime.ofInstant(until, ZoneOffset.UTC).plusHours(14);
    int sent = 0;
    for (Order order : orders.findReminderCandidates(OrderStatus.PAID, from, to)) {
      Instant start = order.getEvent().startsAtInstant();
      if (start.isAfter(now) && !start.isAfter(until)) {
        emails.reminder(order);
        order.setReminderSentAt(now);
        sent++;
      }
    }
    return sent;
  }
}

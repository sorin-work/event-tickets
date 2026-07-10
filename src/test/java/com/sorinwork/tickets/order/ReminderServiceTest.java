package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.notify.OutboxRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class ReminderServiceTest extends IntegrationTest {

  @Autowired ReminderService reminders;
  @Autowired EventService events;
  @Autowired OutboxRepository outbox;

  Event event;
  TicketTier tier;

  @BeforeEach
  void setUp() {
    // Starts 2030-03-14 19:30 in Bucharest = 17:30 UTC.
    event = data.event("Choir Concert");
    tier = data.tier(event, "Seat", "10", 50);
    events.publish(event.getId());
    purchases.buy(tier, 1, "Ana", "ana@example.com");
  }

  private long reminderCount() {
    return outbox.findByRecipientOrderByIdAsc("ana@example.com").stream()
        .filter(m -> m.getSubject().startsWith("Tomorrow"))
        .count();
  }

  @Test
  void remindsBuyersADayBefore() {
    clock.set(Instant.parse("2030-03-13T20:00:00Z"));

    assertThat(reminders.sendDueReminders()).isEqualTo(1);
    assertThat(reminderCount()).isEqualTo(1);
  }

  @Test
  void notTooEarly() {
    clock.set(Instant.parse("2030-03-13T16:00:00Z"));

    assertThat(reminders.sendDueReminders()).isZero();
  }

  @Test
  void onlyOnce() {
    clock.set(Instant.parse("2030-03-13T20:00:00Z"));
    reminders.sendDueReminders();
    clock.set(Instant.parse("2030-03-14T08:00:00Z"));

    assertThat(reminders.sendDueReminders()).isZero();
    assertThat(reminderCount()).isEqualTo(1);
  }

  @Test
  void cancelledEventsGetNoReminder() {
    events.cancel(event.getId());
    clock.set(Instant.parse("2030-03-13T20:00:00Z"));

    assertThat(reminders.sendDueReminders()).isZero();
  }
}

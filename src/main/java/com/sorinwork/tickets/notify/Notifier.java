package com.sorinwork.tickets.notify;

import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Sends emails to buyers by writing them to the outbox. */
@Service
public class Notifier {

  private static final Logger log = LoggerFactory.getLogger(Notifier.class);

  private final OutboxRepository outbox;
  private final Clock clock;

  public Notifier(OutboxRepository outbox, Clock clock) {
    this.outbox = outbox;
    this.clock = clock;
  }

  @Transactional
  public OutboxMessage send(String to, String subject, String body, Long eventId) {
    OutboxMessage message =
        outbox.save(new OutboxMessage(to, subject, body, eventId, clock.instant()));
    log.info("Queued email \"{}\" to {}", subject, to);
    return message;
  }
}

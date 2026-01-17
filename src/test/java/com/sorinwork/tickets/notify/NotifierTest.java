package com.sorinwork.tickets.notify;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class NotifierTest extends IntegrationTest {

  @Autowired Notifier notifier;
  @Autowired OutboxRepository outbox;

  @Test
  void storesMessagesInTheOutbox() {
    notifier.send("ana@example.com", "Hello", "Body text", null);

    assertThat(outbox.findByRecipientOrderByIdAsc("ana@example.com"))
        .singleElement()
        .satisfies(
            m -> {
              assertThat(m.getSubject()).isEqualTo("Hello");
              assertThat(m.getCreatedAt()).isEqualTo(clock.instant());
            });
  }
}

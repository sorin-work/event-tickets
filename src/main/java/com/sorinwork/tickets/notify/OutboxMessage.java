package com.sorinwork.tickets.notify;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * An email the app would send. There is no mail server in the demo setup, so messages are kept here
 * and can be read in the admin area.
 */
@Entity
@Table(name = "outbox_messages")
public class OutboxMessage {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String recipient;

  @Column(nullable = false)
  private String subject;

  @Column(nullable = false)
  private String body;

  @Column(name = "event_id")
  private Long eventId;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected OutboxMessage() {}

  public OutboxMessage(
      String recipient, String subject, String body, Long eventId, Instant createdAt) {
    this.recipient = recipient;
    this.subject = subject;
    this.body = body;
    this.eventId = eventId;
    this.createdAt = createdAt;
  }

  public Long getId() {
    return id;
  }

  public String getRecipient() {
    return recipient;
  }

  public String getSubject() {
    return subject;
  }

  public String getBody() {
    return body;
  }

  public Long getEventId() {
    return eventId;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

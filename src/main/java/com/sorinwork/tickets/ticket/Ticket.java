package com.sorinwork.tickets.ticket;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;

/** One admission. Issued when an order is paid; its code is what gets scanned at the door. */
@Entity
@Table(name = "tickets")
public class Ticket {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tier_id")
  private TicketTier tier;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @Column(nullable = false)
  private String code;

  @Column(name = "attendee_name", nullable = false)
  private String attendeeName;

  @Column(name = "attendee_email")
  private String attendeeEmail;

  @Column(name = "checked_in_at")
  private Instant checkedInAt;

  @Column(name = "checked_in_by")
  private String checkedInBy;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected Ticket() {}

  public Ticket(
      Order order,
      TicketTier tier,
      String code,
      String attendeeName,
      String attendeeEmail,
      Instant createdAt) {
    this.order = order;
    this.tier = tier;
    this.event = order.getEvent();
    this.code = code;
    this.attendeeName = attendeeName;
    this.attendeeEmail = attendeeEmail;
    this.createdAt = createdAt;
  }

  public boolean isCheckedIn() {
    return checkedInAt != null;
  }

  public Long getId() {
    return id;
  }

  public Order getOrder() {
    return order;
  }

  public TicketTier getTier() {
    return tier;
  }

  public Event getEvent() {
    return event;
  }

  public String getCode() {
    return code;
  }

  public String getAttendeeName() {
    return attendeeName;
  }

  public void setAttendeeName(String attendeeName) {
    this.attendeeName = attendeeName;
  }

  public String getAttendeeEmail() {
    return attendeeEmail;
  }

  public void setAttendeeEmail(String attendeeEmail) {
    this.attendeeEmail = attendeeEmail;
  }

  public Instant getCheckedInAt() {
    return checkedInAt;
  }

  public String getCheckedInBy() {
    return checkedInBy;
  }

  public void checkIn(Instant at, String by) {
    this.checkedInAt = at;
    this.checkedInBy = by;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

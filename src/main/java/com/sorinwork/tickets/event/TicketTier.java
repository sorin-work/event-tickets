package com.sorinwork.tickets.event;

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
import java.time.LocalDateTime;

/**
 * A kind of ticket for an event, e.g. "Early bird" or "Standing". Sales start and end are
 * wall-clock times in the event's time zone.
 */
@Entity
@Table(name = "ticket_tiers")
public class TicketTier {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @Column(nullable = false)
  private String name;

  private String description;

  @Column(name = "price_cents", nullable = false)
  private long priceCents;

  @Column(nullable = false)
  private int quantity;

  @Column(name = "max_per_order", nullable = false)
  private int maxPerOrder = 10;

  @Column(name = "sales_start")
  private LocalDateTime salesStart;

  @Column(name = "sales_end")
  private LocalDateTime salesEnd;

  /** Temporarily off sale, e.g. while the organiser checks how many seats are really left. */
  @Column(nullable = false)
  private boolean paused;

  @Column(nullable = false)
  private int position;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected TicketTier() {}

  public TicketTier(Event event, String name, long priceCents, int quantity, Instant createdAt) {
    this.event = event;
    this.name = name;
    this.priceCents = priceCents;
    this.quantity = quantity;
    this.createdAt = createdAt;
  }

  public boolean isFree() {
    return priceCents == 0;
  }

  public Long getId() {
    return id;
  }

  public Event getEvent() {
    return event;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public long getPriceCents() {
    return priceCents;
  }

  public void setPriceCents(long priceCents) {
    this.priceCents = priceCents;
  }

  public int getQuantity() {
    return quantity;
  }

  public void setQuantity(int quantity) {
    this.quantity = quantity;
  }

  public int getMaxPerOrder() {
    return maxPerOrder;
  }

  public void setMaxPerOrder(int maxPerOrder) {
    this.maxPerOrder = maxPerOrder;
  }

  public LocalDateTime getSalesStart() {
    return salesStart;
  }

  public void setSalesStart(LocalDateTime salesStart) {
    this.salesStart = salesStart;
  }

  public LocalDateTime getSalesEnd() {
    return salesEnd;
  }

  public void setSalesEnd(LocalDateTime salesEnd) {
    this.salesEnd = salesEnd;
  }

  public boolean isPaused() {
    return paused;
  }

  public void setPaused(boolean paused) {
    this.paused = paused;
  }

  public int getPosition() {
    return position;
  }

  public void setPosition(int position) {
    this.position = position;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

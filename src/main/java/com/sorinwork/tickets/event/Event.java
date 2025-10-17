package com.sorinwork.tickets.event;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

/**
 * Something people buy tickets for. Start and end are stored as wall-clock times in the event's own
 * time zone, the way organisers think about them.
 */
@Entity
@Table(name = "events")
public class Event {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String slug;

  @Column(nullable = false)
  private String title;

  private String description;

  @Column(name = "venue_name", nullable = false)
  private String venueName;

  @Column(name = "venue_address")
  private String venueAddress;

  @Column(name = "starts_at", nullable = false)
  private LocalDateTime startsAt;

  @Column(name = "ends_at")
  private LocalDateTime endsAt;

  @Column(name = "time_zone", nullable = false)
  private String timeZone;

  @Column(nullable = false)
  private String currency;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private EventStatus status = EventStatus.DRAFT;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  protected Event() {}

  public Event(String slug, String title, Instant createdAt) {
    this.slug = slug;
    this.title = title;
    this.createdAt = createdAt;
    this.updatedAt = createdAt;
  }

  public ZoneId zone() {
    return ZoneId.of(timeZone);
  }

  public Instant startsAtInstant() {
    return startsAt.atZone(zone()).toInstant();
  }

  public boolean isPublished() {
    return status == EventStatus.PUBLISHED;
  }

  public Long getId() {
    return id;
  }

  public String getSlug() {
    return slug;
  }

  public void setSlug(String slug) {
    this.slug = slug;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getVenueName() {
    return venueName;
  }

  public void setVenueName(String venueName) {
    this.venueName = venueName;
  }

  public String getVenueAddress() {
    return venueAddress;
  }

  public void setVenueAddress(String venueAddress) {
    this.venueAddress = venueAddress;
  }

  public LocalDateTime getStartsAt() {
    return startsAt;
  }

  public void setStartsAt(LocalDateTime startsAt) {
    this.startsAt = startsAt;
  }

  public LocalDateTime getEndsAt() {
    return endsAt;
  }

  public void setEndsAt(LocalDateTime endsAt) {
    this.endsAt = endsAt;
  }

  public String getTimeZone() {
    return timeZone;
  }

  public void setTimeZone(String timeZone) {
    this.timeZone = timeZone;
  }

  public String getCurrency() {
    return currency;
  }

  public void setCurrency(String currency) {
    this.currency = currency;
  }

  public EventStatus getStatus() {
    return status;
  }

  public void setStatus(EventStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Instant updatedAt) {
    this.updatedAt = updatedAt;
  }
}

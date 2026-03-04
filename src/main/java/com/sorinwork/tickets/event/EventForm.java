package com.sorinwork.tickets.event;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.springframework.format.annotation.DateTimeFormat;

public class EventForm {

  @NotBlank
  @Size(max = 160)
  private String title;

  @Size(max = 10000)
  private String description;

  @NotBlank
  @Size(max = 160)
  private String venueName;

  @Size(max = 255)
  private String venueAddress;

  @NotNull
  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime startsAt;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime endsAt;

  @Min(1)
  @Max(100000)
  private Integer capacity;

  @NotBlank private String timeZone = "Europe/Bucharest";

  @NotBlank
  @Pattern(regexp = "[A-Z]{3}", message = "must be a three letter currency code")
  private String currency = "EUR";

  public static EventForm from(Event event) {
    EventForm form = new EventForm();
    form.title = event.getTitle();
    form.description = event.getDescription();
    form.venueName = event.getVenueName();
    form.venueAddress = event.getVenueAddress();
    form.startsAt = event.getStartsAt();
    form.endsAt = event.getEndsAt();
    form.timeZone = event.getTimeZone();
    form.currency = event.getCurrency();
    form.capacity = event.getCapacity();
    return form;
  }

  @AssertTrue(message = "must be after the start")
  public boolean isEndAfterStart() {
    return endsAt == null || startsAt == null || endsAt.isAfter(startsAt);
  }

  @AssertTrue(message = "is not a known time zone")
  public boolean isTimeZoneValid() {
    if (timeZone == null || timeZone.isBlank()) {
      return true;
    }
    try {
      ZoneId.of(timeZone);
      return true;
    } catch (DateTimeException e) {
      return false;
    }
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

  public Integer getCapacity() {
    return capacity;
  }

  public void setCapacity(Integer capacity) {
    this.capacity = capacity;
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
}

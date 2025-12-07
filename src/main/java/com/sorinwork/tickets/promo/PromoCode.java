package com.sorinwork.tickets.promo;

import com.sorinwork.tickets.event.Event;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Locale;

@Entity
@Table(name = "promo_codes")
public class PromoCode {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @Column(nullable = false)
  private String code;

  @Enumerated(EnumType.STRING)
  @Column(name = "discount_type", nullable = false)
  private DiscountType discountType;

  @Column(nullable = false)
  private long amount;

  @Column(name = "max_uses")
  private Integer maxUses;

  @Column(name = "used_count", nullable = false)
  private int usedCount;

  /** Last moment the code can be used, wall-clock time in the event's time zone. */
  @Column(name = "valid_until")
  private LocalDateTime validUntil;

  @Column(nullable = false)
  private boolean active = true;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected PromoCode() {}

  public PromoCode(
      Event event, String code, DiscountType discountType, long amount, Instant createdAt) {
    this.event = event;
    this.code = normalize(code);
    this.discountType = discountType;
    this.amount = amount;
    this.createdAt = createdAt;
  }

  public static String normalize(String code) {
    return code == null ? null : code.trim().toUpperCase(Locale.ROOT);
  }

  public boolean isUsedUp() {
    return maxUses != null && usedCount >= maxUses;
  }

  public Long getId() {
    return id;
  }

  public Event getEvent() {
    return event;
  }

  public String getCode() {
    return code;
  }

  public DiscountType getDiscountType() {
    return discountType;
  }

  public void setDiscountType(DiscountType discountType) {
    this.discountType = discountType;
  }

  public long getAmount() {
    return amount;
  }

  public void setAmount(long amount) {
    this.amount = amount;
  }

  public Integer getMaxUses() {
    return maxUses;
  }

  public void setMaxUses(Integer maxUses) {
    this.maxUses = maxUses;
  }

  public int getUsedCount() {
    return usedCount;
  }

  public void setUsedCount(int usedCount) {
    this.usedCount = usedCount;
  }

  public LocalDateTime getValidUntil() {
    return validUntil;
  }

  public void setValidUntil(LocalDateTime validUntil) {
    this.validUntil = validUntil;
  }

  public boolean isActive() {
    return active;
  }

  public void setActive(boolean active) {
    this.active = active;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}

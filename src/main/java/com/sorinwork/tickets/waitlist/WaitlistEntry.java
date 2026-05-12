package com.sorinwork.tickets.waitlist;

import com.sorinwork.tickets.common.Tokens;
import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
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

@Entity
@Table(name = "waitlist_entries")
public class WaitlistEntry {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tier_id")
  private TicketTier tier;

  @Column(nullable = false)
  private String name;

  @Column(nullable = false)
  private String email;

  @Column(nullable = false)
  private int quantity;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private WaitlistStatus status = WaitlistStatus.WAITING;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  /** Secret part of the "leave the waitlist" link sent to the person. */
  @Column(name = "leave_token")
  private String leaveToken;

  @Column(name = "offered_at")
  private Instant offeredAt;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "offer_order_id")
  private Order offerOrder;

  protected WaitlistEntry() {}

  public WaitlistEntry(
      TicketTier tier, String name, String email, int quantity, Instant createdAt) {
    this.event = tier.getEvent();
    this.tier = tier;
    this.name = name;
    this.email = email;
    this.quantity = quantity;
    this.createdAt = createdAt;
    this.leaveToken = Tokens.accessToken();
  }

  public void offer(Order order, Instant at) {
    this.status = WaitlistStatus.OFFERED;
    this.offerOrder = order;
    this.offeredAt = at;
  }

  public Long getId() {
    return id;
  }

  public Event getEvent() {
    return event;
  }

  public TicketTier getTier() {
    return tier;
  }

  public String getName() {
    return name;
  }

  public String getEmail() {
    return email;
  }

  public int getQuantity() {
    return quantity;
  }

  public WaitlistStatus getStatus() {
    return status;
  }

  public void setStatus(WaitlistStatus status) {
    this.status = status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public String getLeaveToken() {
    return leaveToken;
  }

  public Instant getOfferedAt() {
    return offeredAt;
  }

  public Order getOfferOrder() {
    return offerOrder;
  }
}

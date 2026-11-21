package com.sorinwork.tickets.order;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.promo.PromoCode;
import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders")
public class Order {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  /** Short code shown to buyers and used by staff, e.g. {@code ET-7K3F9Q}. */
  @Column(nullable = false)
  private String reference;

  /** Secret part of the buyer's links to checkout and tickets. */
  @Column(name = "access_token", nullable = false)
  private String accessToken;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "event_id")
  private Event event;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private OrderStatus status = OrderStatus.PENDING;

  @Column(name = "buyer_name")
  private String buyerName;

  @Column(name = "buyer_email")
  private String buyerEmail;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "promo_code_id")
  private PromoCode promoCode;

  @Column(name = "subtotal_cents", nullable = false)
  private long subtotalCents;

  @Column(name = "discount_cents", nullable = false)
  private long discountCents;

  @Column(name = "total_cents", nullable = false)
  private long totalCents;

  @Column(name = "hold_expires_at", nullable = false)
  private Instant holdExpiresAt;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "paid_at")
  private Instant paidAt;

  @Column(name = "cancelled_at")
  private Instant cancelledAt;

  @Column(name = "reminder_sent_at")
  private Instant reminderSentAt;

  /** Keyed hash of the address that started the checkout; null for waitlist offers. */
  @Column(name = "client_key")
  private String clientKey;

  /** Internal note, only shown to admins. */
  @Column(name = "admin_note")
  private String adminNote;

  /** Guards against two requests completing the same checkout at once. */
  @Version private long version;

  @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
  @OrderBy("id")
  private List<OrderItem> items = new ArrayList<>();

  protected Order() {}

  public Order(
      Event event, String reference, String accessToken, Instant createdAt, Instant holdExpiresAt) {
    this.event = event;
    this.reference = reference;
    this.accessToken = accessToken;
    this.createdAt = createdAt;
    this.holdExpiresAt = holdExpiresAt;
  }

  public void addItem(OrderItem item) {
    items.add(item);
    subtotalCents += item.lineTotalCents();
    totalCents = subtotalCents - discountCents;
  }

  public int ticketCount() {
    return items.stream().mapToInt(OrderItem::getQuantity).sum();
  }

  /** A pending order whose hold has not run out yet. */
  public boolean isHoldActive(Instant now) {
    return status == OrderStatus.PENDING && holdExpiresAt.isAfter(now);
  }

  public Long getId() {
    return id;
  }

  public String getReference() {
    return reference;
  }

  public String getAccessToken() {
    return accessToken;
  }

  public Event getEvent() {
    return event;
  }

  public OrderStatus getStatus() {
    return status;
  }

  public void setStatus(OrderStatus status) {
    this.status = status;
  }

  public String getBuyerName() {
    return buyerName;
  }

  public void setBuyerName(String buyerName) {
    this.buyerName = buyerName;
  }

  public String getBuyerEmail() {
    return buyerEmail;
  }

  public void setBuyerEmail(String buyerEmail) {
    this.buyerEmail = buyerEmail;
  }

  public PromoCode getPromoCode() {
    return promoCode;
  }

  public void setPromoCode(PromoCode promoCode) {
    this.promoCode = promoCode;
  }

  public long getSubtotalCents() {
    return subtotalCents;
  }

  public long getDiscountCents() {
    return discountCents;
  }

  public void setDiscountCents(long discountCents) {
    this.discountCents = discountCents;
    this.totalCents = subtotalCents - discountCents;
  }

  public long getTotalCents() {
    return totalCents;
  }

  public Instant getHoldExpiresAt() {
    return holdExpiresAt;
  }

  public void setHoldExpiresAt(Instant holdExpiresAt) {
    this.holdExpiresAt = holdExpiresAt;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getPaidAt() {
    return paidAt;
  }

  public void setPaidAt(Instant paidAt) {
    this.paidAt = paidAt;
  }

  public Instant getCancelledAt() {
    return cancelledAt;
  }

  public void setCancelledAt(Instant cancelledAt) {
    this.cancelledAt = cancelledAt;
  }

  public Instant getReminderSentAt() {
    return reminderSentAt;
  }

  public void setReminderSentAt(Instant reminderSentAt) {
    this.reminderSentAt = reminderSentAt;
  }

  public String getClientKey() {
    return clientKey;
  }

  public void setClientKey(String clientKey) {
    this.clientKey = clientKey;
  }

  public String getAdminNote() {
    return adminNote;
  }

  public void setAdminNote(String adminNote) {
    this.adminNote = adminNote;
  }

  public List<OrderItem> getItems() {
    return items;
  }
}

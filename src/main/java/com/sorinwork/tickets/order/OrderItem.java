package com.sorinwork.tickets.order;

import com.sorinwork.tickets.event.TicketTier;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "order_items")
public class OrderItem {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "order_id")
  private Order order;

  @ManyToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "tier_id")
  private TicketTier tier;

  @Column(nullable = false)
  private int quantity;

  /** Price at the time of the order, so later price changes do not alter it. */
  @Column(name = "unit_price_cents", nullable = false)
  private long unitPriceCents;

  protected OrderItem() {}

  public OrderItem(Order order, TicketTier tier, int quantity) {
    this.order = order;
    this.tier = tier;
    this.quantity = quantity;
    this.unitPriceCents = tier.getPriceCents();
  }

  public long lineTotalCents() {
    return unitPriceCents * quantity;
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

  public int getQuantity() {
    return quantity;
  }

  public long getUnitPriceCents() {
    return unitPriceCents;
  }
}

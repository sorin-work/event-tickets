package com.sorinwork.tickets.promo;

public enum DiscountType {
  /** {@code amount} is a percentage of the order, 1 to 100. */
  PERCENT,
  /** {@code amount} is a fixed reduction in cents. */
  FIXED
}

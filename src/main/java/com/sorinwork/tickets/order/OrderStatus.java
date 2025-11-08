package com.sorinwork.tickets.order;

public enum OrderStatus {
  /** Seats are held while the buyer completes checkout. */
  PENDING,
  /** Paid; tickets have been issued. */
  PAID,
  /** The hold ran out before checkout was completed. */
  EXPIRED,
  /** Cancelled by an admin; its tickets are no longer valid. */
  CANCELLED
}

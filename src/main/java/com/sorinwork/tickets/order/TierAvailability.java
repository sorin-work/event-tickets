package com.sorinwork.tickets.order;

/** How many tickets of a tier are sold, held in open checkouts and still available. */
public record TierAvailability(long tierId, int quantity, int sold, int held) {

  public int remaining() {
    return Math.max(0, quantity - sold - held);
  }

  public boolean isSoldOut() {
    return remaining() == 0;
  }
}

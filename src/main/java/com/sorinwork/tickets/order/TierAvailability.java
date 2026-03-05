package com.sorinwork.tickets.order;

/**
 * How many tickets of a tier are sold, held in open checkouts and still available. {@code
 * eventRemaining} is what is left of the event's venue capacity, shared by all tiers.
 */
public record TierAvailability(long tierId, int quantity, int sold, int held, int eventRemaining) {

  public TierAvailability(long tierId, int quantity, int sold, int held) {
    this(tierId, quantity, sold, held, Integer.MAX_VALUE);
  }

  public int remaining() {
    return Math.max(0, Math.min(quantity - sold - held, eventRemaining));
  }

  public boolean isSoldOut() {
    return remaining() == 0;
  }
}

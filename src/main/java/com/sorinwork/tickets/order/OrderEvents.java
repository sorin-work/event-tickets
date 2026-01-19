package com.sorinwork.tickets.order;

/** Things that happen to orders, for other parts of the app to react to. */
public final class OrderEvents {

  private OrderEvents() {}

  /** A checkout hold ran out without payment. */
  public record HoldExpired(long orderId) {}

  /** Tickets of the event went back on sale, e.g. after holds expired. */
  public record TicketsReleased(long eventId) {}
}

package com.sorinwork.tickets.report;

import java.util.List;

/** Sales figures of one event. Amounts are in cents of the event currency. */
public record SalesReport(
    List<TierLine> tiers,
    long orders,
    long ticketsSold,
    long grossCents,
    long discountCents,
    long netCents,
    long checkedIn,
    long cancelledOrders) {

  public record TierLine(String name, long priceCents, int capacity, int sold, long grossCents) {

    public int percentSold() {
      return capacity == 0 ? 0 : sold * 100 / capacity;
    }
  }

  public int capacity() {
    return tiers.stream().mapToInt(TierLine::capacity).sum();
  }

  public int percentCheckedIn() {
    return ticketsSold == 0 ? 0 : (int) (checkedIn * 100 / ticketsSold);
  }
}

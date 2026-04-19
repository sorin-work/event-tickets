package com.sorinwork.tickets.report;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.order.OrderItemRepository;
import com.sorinwork.tickets.order.OrderRepository;
import com.sorinwork.tickets.order.OrderStatus;
import com.sorinwork.tickets.order.TierAvailability;
import com.sorinwork.tickets.ticket.CheckInService;
import com.sorinwork.tickets.ticket.TicketRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class SalesReportService {

  private final TicketTierRepository tiers;
  private final OrderRepository orders;
  private final OrderItemRepository items;
  private final AvailabilityService availability;
  private final CheckInService checkIn;
  private final TicketRepository tickets;

  public SalesReportService(
      TicketTierRepository tiers,
      OrderRepository orders,
      OrderItemRepository items,
      AvailabilityService availability,
      CheckInService checkIn,
      TicketRepository tickets) {
    this.tiers = tiers;
    this.orders = orders;
    this.items = items;
    this.availability = availability;
    this.checkIn = checkIn;
    this.tickets = tickets;
  }

  public SalesReport forEvent(long eventId) {
    Map<Long, TierAvailability> counts = availability.forEvent(eventId);
    Map<Long, Long> revenue = new HashMap<>();
    for (var row : items.revenueByTier(eventId, OrderStatus.PAID)) {
      revenue.put(row.getTierId(), row.getGross());
    }
    List<SalesReport.TierLine> lines = new ArrayList<>();
    long sold = 0;
    for (TicketTier tier : tiers.findByEventIdOrderByPositionAscIdAsc(eventId)) {
      TierAvailability count = counts.get(tier.getId());
      lines.add(
          new SalesReport.TierLine(
              tier.getName(),
              tier.getPriceCents(),
              tier.getQuantity(),
              count.sold(),
              revenue.getOrDefault(tier.getId(), 0L)));
      sold += count.sold();
    }
    var totals = orders.totalsForEvent(eventId, OrderStatus.PAID);
    return new SalesReport(
        lines,
        totals.getOrders(),
        sold,
        totals.getSubtotal(),
        totals.getDiscount(),
        totals.getTotal(),
        checkIn.checkedInCount(eventId),
        orders.countByEventIdAndStatus(eventId, OrderStatus.CANCELLED));
  }

  /** Check-ins per hour of the door opening, in the event's time zone, e.g. 19:00 -> 42. */
  public Map<java.time.LocalDateTime, Long> checkInsByHour(Event event) {
    Map<java.time.LocalDateTime, Long> hours = new TreeMap<>();
    for (java.time.Instant at : tickets.checkInTimes(event.getId())) {
      java.time.LocalDateTime hour =
          at.atZone(event.zone())
              .toLocalDateTime()
              .truncatedTo(java.time.temporal.ChronoUnit.HOURS);
      hours.merge(hour, 1L, Long::sum);
    }
    return hours;
  }

  /** Sales grouped by the day they were paid, oldest first. Days without sales are left out. */
  public List<DailySales> dailySales(Event event) {
    Map<LocalDate, long[]> days = new TreeMap<>();
    for (var row : orders.paidOrderRows(event.getId(), OrderStatus.PAID)) {
      LocalDate day = row.getPaidAt().atZone(event.zone()).toLocalDate();
      long[] totals = days.computeIfAbsent(day, d -> new long[2]);
      totals[0] += row.getTickets();
      totals[1] += row.getTotal();
    }
    List<DailySales> result = new ArrayList<>();
    days.forEach((day, totals) -> result.add(new DailySales(day, totals[0], totals[1])));
    return result;
  }

  /** Paid orders and total discount per promo code, most used first. */
  public List<PromoUsage> promoUsage(long eventId) {
    return orders.promoUsage(eventId, OrderStatus.PAID).stream()
        .map(row -> new PromoUsage(row.getCode(), row.getOrders(), row.getDiscount()))
        .toList();
  }

  public record PromoUsage(String code, long orders, long discountCents) {}
}

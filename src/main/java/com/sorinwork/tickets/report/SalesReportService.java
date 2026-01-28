package com.sorinwork.tickets.report;

import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.event.TicketTierRepository;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.order.OrderItemRepository;
import com.sorinwork.tickets.order.OrderRepository;
import com.sorinwork.tickets.order.OrderStatus;
import com.sorinwork.tickets.order.TierAvailability;
import com.sorinwork.tickets.ticket.CheckInService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  public SalesReportService(
      TicketTierRepository tiers,
      OrderRepository orders,
      OrderItemRepository items,
      AvailabilityService availability,
      CheckInService checkIn) {
    this.tiers = tiers;
    this.orders = orders;
    this.items = items;
    this.availability = availability;
    this.checkIn = checkIn;
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
}

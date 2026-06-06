package com.sorinwork.tickets.order;

import com.sorinwork.tickets.common.Csv;
import com.sorinwork.tickets.common.Money;
import java.io.Writer;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Orders of an event as CSV, for bookkeeping. */
@Service
@Transactional(readOnly = true)
public class OrderExport {

  private final OrderRepository orders;

  public OrderExport(OrderRepository orders) {
    this.orders = orders;
  }

  /** Paid and cancelled orders, oldest first. Times are in the event's zone. */
  public void writeCsv(long eventId, ZoneId zone, Writer out) {
    DateTimeFormatter time = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(zone);
    Csv csv = new Csv(out);
    csv.row(
        "Order",
        "Status",
        "Buyer",
        "Email",
        "Tickets",
        "Subtotal",
        "Discount",
        "Total",
        "Promo code",
        "Paid at");
    List<Order> all =
        new ArrayList<>(
            orders
                .findForEvent(
                    eventId, List.of(OrderStatus.PAID, OrderStatus.CANCELLED), Pageable.unpaged())
                .getContent());
    Collections.reverse(all);
    for (Order order : all) {
      csv.row(
          order.getReference(),
          order.getStatus(),
          order.getBuyerName(),
          order.getBuyerEmail(),
          order.ticketCount(),
          Money.toDecimal(order.getSubtotalCents()),
          Money.toDecimal(order.getDiscountCents()),
          Money.toDecimal(order.getTotalCents()),
          order.getPromoCode() == null ? "" : order.getPromoCode().getCode(),
          order.getPaidAt() == null ? "" : time.format(order.getPaidAt()));
    }
  }
}

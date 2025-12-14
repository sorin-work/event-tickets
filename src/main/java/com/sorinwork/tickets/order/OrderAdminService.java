package com.sorinwork.tickets.order;

import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.ticket.TicketRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order lookups and changes for the admin area. */
@Service
@Transactional
public class OrderAdminService {

  static final List<OrderStatus> VISIBLE =
      List.of(OrderStatus.PAID, OrderStatus.PENDING, OrderStatus.CANCELLED);

  private final OrderRepository orders;
  private final TicketRepository tickets;

  public OrderAdminService(OrderRepository orders, TicketRepository tickets) {
    this.orders = orders;
    this.tickets = tickets;
  }

  /** Orders of an event, newest first; expired checkouts are left out. */
  @Transactional(readOnly = true)
  public List<Order> forEvent(long eventId) {
    return orders.findForEvent(eventId, VISIBLE);
  }

  @Transactional(readOnly = true)
  public Order get(long orderId) {
    return orders
        .findWithItemsById(orderId)
        .orElseThrow(() -> new NotFoundException("Order " + orderId));
  }

  @Transactional(readOnly = true)
  public List<Ticket> tickets(long orderId) {
    return tickets.findByOrderIdOrderById(orderId);
  }
}

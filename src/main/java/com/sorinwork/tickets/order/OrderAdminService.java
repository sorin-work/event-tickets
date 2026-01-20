package com.sorinwork.tickets.order;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.ticket.TicketRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
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
  private final ApplicationEventPublisher publisher;
  private final Clock clock;

  public OrderAdminService(
      OrderRepository orders,
      TicketRepository tickets,
      ApplicationEventPublisher publisher,
      Clock clock) {
    this.orders = orders;
    this.tickets = tickets;
    this.publisher = publisher;
    this.clock = clock;
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

  /**
   * Cancels an order, e.g. after a refund outside the app. Its tickets stop working at the door and
   * the seats go back on sale.
   */
  public Order cancel(long orderId) {
    Order order = get(orderId);
    if (order.getStatus() == OrderStatus.CANCELLED) {
      throw new BusinessException("This order is already cancelled.");
    }
    if (order.getStatus() == OrderStatus.EXPIRED) {
      throw new BusinessException("Expired checkouts cannot be cancelled.");
    }
    order.setStatus(OrderStatus.CANCELLED);
    order.setCancelledAt(clock.instant());
    orders.flush();
    publisher.publishEvent(new OrderEvents.TicketsReleased(order.getEvent().getId()));
    return order;
  }
}

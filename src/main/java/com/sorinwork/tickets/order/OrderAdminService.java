package com.sorinwork.tickets.order;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.common.NotFoundException;
import com.sorinwork.tickets.common.PageView;
import com.sorinwork.tickets.notify.Emails;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.ticket.TicketRepository;
import java.time.Clock;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Order lookups and changes for the admin area. */
@Service
@Transactional
public class OrderAdminService {

  public static final int PAGE_SIZE = 30;

  static final List<OrderStatus> VISIBLE =
      List.of(OrderStatus.PAID, OrderStatus.PENDING, OrderStatus.CANCELLED);

  private final OrderRepository orders;
  private final TicketRepository tickets;
  private final ApplicationEventPublisher publisher;
  private final Emails emails;
  private final Clock clock;

  public OrderAdminService(
      OrderRepository orders,
      TicketRepository tickets,
      ApplicationEventPublisher publisher,
      Emails emails,
      Clock clock) {
    this.orders = orders;
    this.tickets = tickets;
    this.publisher = publisher;
    this.emails = emails;
    this.clock = clock;
  }

  /** Sends the buyer their ticket link again, e.g. when the first email went to spam. */
  public Order resendConfirmation(long orderId) {
    Order order = get(orderId);
    if (order.getStatus() != OrderStatus.PAID) {
      throw new BusinessException("Only paid orders have tickets to send.");
    }
    emails.orderConfirmation(order);
    return order;
  }

  /** Orders of an event, newest first; expired checkouts are left out. */
  @Transactional(readOnly = true)
  public PageView<Order> forEvent(long eventId, int page) {
    return forEvent(eventId, null, page);
  }

  /**
   * A page of the event's orders with the given status, or all visible ones when {@code status} is
   * null. {@code page} is 1-based.
   */
  @Transactional(readOnly = true)
  public PageView<Order> forEvent(long eventId, OrderStatus status, int page) {
    return PageView.of(
        orders.findForEvent(
            eventId,
            status == null ? VISIBLE : List.of(status),
            PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE)));
  }

  /** The latest paid orders across all events, for the dashboard. */
  @Transactional(readOnly = true)
  public List<Order> recentPaid(int limit) {
    return orders.findRecent(OrderStatus.PAID, PageRequest.of(0, limit));
  }

  @Transactional(readOnly = true)
  public long paidSince(java.time.Duration period) {
    return orders.countByStatusAndPaidAtAfter(OrderStatus.PAID, clock.instant().minus(period));
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

  @Transactional(readOnly = true)
  public long ticketOrderId(long ticketId) {
    return tickets
        .findById(ticketId)
        .map(t -> t.getOrder().getId())
        .orElseThrow(() -> new NotFoundException("Ticket " + ticketId));
  }

  /** Changes the name on a ticket, e.g. when a buyer gives theirs to a friend. */
  public Ticket renameTicket(long ticketId, String name) {
    String trimmed = name == null ? "" : name.trim();
    if (trimmed.isEmpty() || trimmed.length() > 120) {
      throw new BusinessException("Enter a name of up to 120 characters.");
    }
    Ticket ticket =
        tickets.findById(ticketId).orElseThrow(() -> new NotFoundException("Ticket " + ticketId));
    ticket.setAttendeeName(trimmed);
    return ticket;
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
    boolean wasPaid = order.getStatus() == OrderStatus.PAID;
    order.setCancelledAt(clock.instant());
    order.setStatus(OrderStatus.CANCELLED);
    if (wasPaid && order.getBuyerEmail() != null) {
      emails.orderCancelled(order);
    }
    orders.flush();
    publisher.publishEvent(new OrderEvents.TicketsReleased(order.getEvent().getId()));
    return order;
  }
}

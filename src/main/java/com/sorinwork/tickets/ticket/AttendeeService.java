package com.sorinwork.tickets.ticket;

import com.sorinwork.tickets.common.PageView;
import com.sorinwork.tickets.order.OrderStatus;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AttendeeService {

  public static final int PAGE_SIZE = 25;

  private final TicketRepository tickets;

  public AttendeeService(TicketRepository tickets) {
    this.tickets = tickets;
  }

  /** Holders of valid tickets for the event, by name. {@code page} is 1-based. */
  public PageView<Ticket> attendees(long eventId, int page) {
    PageRequest request = PageRequest.of(Math.max(page, 1) - 1, PAGE_SIZE);
    return PageView.of(tickets.findAttendees(eventId, OrderStatus.PAID, request));
  }
}

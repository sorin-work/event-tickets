package com.sorinwork.tickets.order;

import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Marks checkouts whose hold ran out as expired. Availability already ignores stale holds; this
 * keeps order statuses honest for admins and reports.
 */
@Service
public class HoldExpiryService {

  private static final Logger log = LoggerFactory.getLogger(HoldExpiryService.class);

  private final OrderRepository orders;
  private final ApplicationEventPublisher publisher;
  private final Clock clock;

  public HoldExpiryService(
      OrderRepository orders, ApplicationEventPublisher publisher, Clock clock) {
    this.orders = orders;
    this.publisher = publisher;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${app.checkout.expiry-interval:30s}", initialDelay = 10_000)
  public void run() {
    int expired = expireHolds();
    if (expired > 0) {
      log.info("Expired {} unpaid checkout(s)", expired);
    }
  }

  @Transactional
  public int expireHolds() {
    Instant now = clock.instant();
    List<Order> stale = orders.lockRunOutHolds(OrderStatus.PENDING, now);
    Set<Long> events = new LinkedHashSet<>();
    for (Order order : stale) {
      order.setStatus(OrderStatus.EXPIRED);
      events.add(order.getEvent().getId());
    }
    orders.flush();
    for (Order order : stale) {
      publisher.publishEvent(new OrderEvents.HoldExpired(order.getId()));
    }
    for (Long eventId : events) {
      publisher.publishEvent(new OrderEvents.TicketsReleased(eventId));
    }
    return stale.size();
  }
}

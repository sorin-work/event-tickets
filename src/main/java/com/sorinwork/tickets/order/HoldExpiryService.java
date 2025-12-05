package com.sorinwork.tickets.order;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
  private final Clock clock;

  public HoldExpiryService(OrderRepository orders, Clock clock) {
    this.orders = orders;
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
    List<Order> stale = orders.findByStatusAndHoldExpiresAtLessThanEqual(OrderStatus.PENDING, now);
    for (Order order : stale) {
      order.setStatus(OrderStatus.EXPIRED);
    }
    return stale.size();
  }
}

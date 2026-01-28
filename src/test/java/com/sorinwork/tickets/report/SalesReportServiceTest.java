package com.sorinwork.tickets.report;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.CheckoutForm;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.promo.DiscountType;
import com.sorinwork.tickets.promo.PromoCode;
import com.sorinwork.tickets.promo.PromoCodeRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.CheckInService;
import com.sorinwork.tickets.ticket.TicketRepository;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class SalesReportServiceTest extends IntegrationTest {

  @Autowired SalesReportService reports;
  @Autowired EventService events;
  @Autowired CheckoutService checkout;
  @Autowired OrderAdminService orders;
  @Autowired PromoCodeRepository promos;
  @Autowired CheckInService checkIn;
  @Autowired TicketRepository tickets;

  Event event;
  TicketTier standing;
  TicketTier vip;

  @BeforeEach
  void setUp() {
    event = data.event("Rock Night");
    standing = data.tier(event, "Standing", "20", 100);
    vip = data.tier(event, "VIP", "50", 10);
    event = events.publish(event.getId());
  }

  @Test
  void summarisesPaidOrders() {
    purchases.buy(standing, 3, "Ana", "ana@example.com");
    Order vipOrder = purchases.buy(vip, 2, "Bogdan", "bogdan@example.com");
    promos.save(new PromoCode(event, "TEN", DiscountType.FIXED, 1000, clock.instant()));
    Order discounted = checkout.start(event.getId(), Map.of(standing.getId(), 1));
    checkout.applyPromoCode(discounted.getAccessToken(), "TEN");
    checkout.complete(discounted.getAccessToken(), new CheckoutForm("Cara", "cara@example.com"));
    Order cancelled = purchases.buy(standing, 5, "Dan", "dan@example.com");
    orders.cancel(cancelled.getId());
    checkout.start(event.getId(), Map.of(standing.getId(), 4));
    checkIn.checkIn(
        event.getId(), tickets.findByOrderIdOrderById(vipOrder.getId()).get(0).getCode(), "door");

    SalesReport report = reports.forEvent(event.getId());

    assertThat(report.tiers())
        .containsExactly(
            new SalesReport.TierLine("Standing", 2000, 100, 4, 8000),
            new SalesReport.TierLine("VIP", 5000, 10, 2, 10000));
    assertThat(report.orders()).isEqualTo(3);
    assertThat(report.ticketsSold()).isEqualTo(6);
    assertThat(report.grossCents()).isEqualTo(18000);
    assertThat(report.discountCents()).isEqualTo(1000);
    assertThat(report.netCents()).isEqualTo(17000);
    assertThat(report.checkedIn()).isEqualTo(1);
    assertThat(report.cancelledOrders()).isEqualTo(1);
    assertThat(report.capacity()).isEqualTo(110);
  }

  @Test
  void emptyEvent() {
    SalesReport report = reports.forEvent(event.getId());

    assertThat(report.ticketsSold()).isZero();
    assertThat(report.netCents()).isZero();
    assertThat(report.percentCheckedIn()).isZero();
  }
}

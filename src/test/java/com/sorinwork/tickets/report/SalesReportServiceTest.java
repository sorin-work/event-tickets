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
import java.time.Instant;
import java.time.LocalDate;
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
  void countsArrivalsPerHourInTheEventTimeZone() {
    Order order = purchases.buy(standing, 3, "Ana", "ana@example.com");
    var codes = tickets.findByOrderIdOrderById(order.getId());
    clock.set(java.time.Instant.parse("2030-03-14T16:55:00Z"));
    checkIn.checkIn(event.getId(), codes.get(0).getCode(), "door");
    clock.set(java.time.Instant.parse("2030-03-14T17:10:00Z"));
    checkIn.checkIn(event.getId(), codes.get(1).getCode(), "door");
    checkIn.checkIn(event.getId(), codes.get(2).getCode(), "door");

    assertThat(reports.checkInsByHour(event))
        .containsExactly(
            java.util.Map.entry(java.time.LocalDateTime.of(2030, 3, 14, 18, 0), 1L),
            java.util.Map.entry(java.time.LocalDateTime.of(2030, 3, 14, 19, 0), 2L));
  }

  @Test
  void emptyEvent() {
    SalesReport report = reports.forEvent(event.getId());

    assertThat(report.ticketsSold()).isZero();
    assertThat(report.netCents()).isZero();
    assertThat(report.percentCheckedIn()).isZero();
  }

  @Test
  void groupsSalesByDayInTheEventTimeZone() {
    // 23:30 UTC is already the next day in Bucharest (UTC+2 in January)
    clock.set(Instant.parse("2030-01-15T21:30:00Z"));
    purchases.buy(standing, 2, "Ana", "ana@example.com");
    clock.set(Instant.parse("2030-01-15T23:30:00Z"));
    purchases.buy(standing, 1, "Bogdan", "bogdan@example.com");
    clock.set(Instant.parse("2030-01-16T09:00:00Z"));
    purchases.buy(vip, 1, "Cara", "cara@example.com");

    assertThat(reports.dailySales(event))
        .containsExactly(
            new DailySales(LocalDate.of(2030, 1, 15), 2, 4000),
            new DailySales(LocalDate.of(2030, 1, 16), 2, 7000));
  }

  @Test
  void countsPromoCodeUseOnPaidOrders() {
    promos.save(new PromoCode(event, "TEN", DiscountType.FIXED, 1000, clock.instant()));
    promos.save(new PromoCode(event, "HALF", DiscountType.PERCENT, 50, clock.instant()));
    for (String code : new String[] {"TEN", "TEN", "HALF"}) {
      Order order = checkout.start(event.getId(), Map.of(standing.getId(), 1));
      checkout.applyPromoCode(order.getAccessToken(), code);
      checkout.complete(order.getAccessToken(), new CheckoutForm("X", "x@example.com"));
    }
    Order unpaid = checkout.start(event.getId(), Map.of(standing.getId(), 1));
    checkout.applyPromoCode(unpaid.getAccessToken(), "HALF");

    assertThat(reports.promoUsage(event.getId()))
        .containsExactly(
            new SalesReportService.PromoUsage("TEN", 2, 2000),
            new SalesReportService.PromoUsage("HALF", 1, 1000));
  }
}

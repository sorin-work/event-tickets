package com.sorinwork.tickets.promo;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.CheckoutForm;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class PromoCheckoutTest extends IntegrationTest {

  @Autowired CheckoutService checkout;
  @Autowired EventService events;
  @Autowired PromoCodeRepository promos;

  Event event;
  TicketTier tier;

  @BeforeEach
  void setUp() {
    event = data.event("Synth Workshop");
    tier = data.tier(event, "Seat", "40", 30);
    events.publish(event.getId());
  }

  private PromoCode promo(String code, DiscountType type, long amount) {
    return promos.save(new PromoCode(event, code, type, amount, clock.instant()));
  }

  private Order order(int quantity) {
    return checkout.start(event.getId(), Map.of(tier.getId(), quantity));
  }

  @Test
  void appliesPercentageDiscount() {
    promo("FRIENDS", DiscountType.PERCENT, 25);

    Order order = checkout.applyPromoCode(order(2).getAccessToken(), " friends ");

    assertThat(order.getDiscountCents()).isEqualTo(2000);
    assertThat(order.getTotalCents()).isEqualTo(6000);
  }

  @Test
  void appliesFixedDiscount() {
    promo("TENOFF", DiscountType.FIXED, 1000);

    Order order = checkout.applyPromoCode(order(1).getAccessToken(), "TENOFF");

    assertThat(order.getTotalCents()).isEqualTo(3000);
  }

  @Test
  void rejectsUnknownAndDisabledCodes() {
    PromoCode off = promo("OFF", DiscountType.PERCENT, 10);
    off.setActive(false);
    promos.save(off);
    String token = order(1).getAccessToken();

    assertThatThrownBy(() -> checkout.applyPromoCode(token, "NOPE"))
        .hasMessage("That promo code is not valid.");
    assertThatThrownBy(() -> checkout.applyPromoCode(token, "OFF"))
        .hasMessage("That promo code is not valid.");
  }

  @Test
  void rejectsExpiredCodes() {
    PromoCode promo = promo("EARLY", DiscountType.PERCENT, 10);
    promo.setValidUntil(LocalDateTime.of(2030, 1, 1, 0, 0));
    promos.save(promo);

    assertThatThrownBy(() -> checkout.applyPromoCode(order(1).getAccessToken(), "EARLY"))
        .hasMessage("That promo code has expired.");
  }

  @Test
  void codesFromOtherEventsDoNotWork() {
    Event other = data.event("Other");
    promos.save(new PromoCode(other, "OTHER", DiscountType.PERCENT, 50, clock.instant()));

    assertThatThrownBy(() -> checkout.applyPromoCode(order(1).getAccessToken(), "OTHER"))
        .hasMessage("That promo code is not valid.");
  }

  @Test
  void limitedCodesRunOut() {
    PromoCode promo = promo("ONCE", DiscountType.PERCENT, 50);
    promo.setMaxUses(1);
    promos.save(promo);
    checkout.applyPromoCode(order(1).getAccessToken(), "ONCE");

    assertThatThrownBy(() -> checkout.applyPromoCode(order(1).getAccessToken(), "ONCE"))
        .hasMessage("That promo code has been used up.");
  }

  @Test
  void replacingACodeFreesTheOldOne() {
    PromoCode first = promo("FIRST", DiscountType.PERCENT, 10);
    promo("SECOND", DiscountType.PERCENT, 20);
    String token = order(1).getAccessToken();

    checkout.applyPromoCode(token, "FIRST");
    Order order = checkout.applyPromoCode(token, "SECOND");

    assertThat(order.getDiscountCents()).isEqualTo(800);
    assertThat(promos.findById(first.getId()).orElseThrow().getUsedCount()).isZero();
  }

  @Test
  void removingTheCodeRestoresTheTotal() {
    promo("FRIENDS", DiscountType.PERCENT, 25);
    String token = order(1).getAccessToken();
    checkout.applyPromoCode(token, "FRIENDS");

    Order order = checkout.removePromoCode(token);

    assertThat(order.getDiscountCents()).isZero();
    assertThat(order.getTotalCents()).isEqualTo(4000);
  }

  @Test
  void discountIsKeptOnThePaidOrder() {
    promo("FRIENDS", DiscountType.PERCENT, 25);
    String token = order(1).getAccessToken();
    checkout.applyPromoCode(token, "FRIENDS");

    Order paid = checkout.complete(token, new CheckoutForm("Ana", "ana@example.com"));

    assertThat(paid.getTotalCents()).isEqualTo(3000);
    assertThat(paid.getPromoCode().getCode()).isEqualTo("FRIENDS");
  }

  @Test
  void cannotApplyAfterTheHoldRanOut() {
    promo("FRIENDS", DiscountType.PERCENT, 25);
    String token = order(1).getAccessToken();
    clock.advance(Duration.ofMinutes(11));

    assertThatThrownBy(() -> checkout.applyPromoCode(token, "FRIENDS"))
        .hasMessageContaining("expired");
  }
}

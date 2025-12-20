package com.sorinwork.tickets.support;

import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.CheckoutForm;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.Order;
import java.util.Map;
import org.springframework.stereotype.Component;

/** Buys tickets through the real checkout, for tests that need paid orders. */
@Component
public class Purchases {

  private final CheckoutService checkout;

  public Purchases(CheckoutService checkout) {
    this.checkout = checkout;
  }

  public Order buy(TicketTier tier, int quantity, String name, String email) {
    Order order = checkout.start(tier.getEvent().getId(), Map.of(tier.getId(), quantity));
    return checkout.complete(order.getAccessToken(), new CheckoutForm(name, email));
  }
}

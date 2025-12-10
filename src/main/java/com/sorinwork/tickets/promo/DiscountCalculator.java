package com.sorinwork.tickets.promo;

/** Works out how much a promo code takes off an order subtotal. */
public final class DiscountCalculator {

  private DiscountCalculator() {}

  /**
   * @return the discount in cents, never more than the subtotal
   */
  public static long discount(DiscountType type, long amount, long subtotalCents) {
    if (subtotalCents <= 0) {
      return 0;
    }
    long discount =
        switch (type) {
          case PERCENT -> (long) (subtotalCents * (Math.min(amount, 100) / 100.0));
          case FIXED -> amount;
        };
    return Math.min(discount, subtotalCents);
  }

  public static long discount(PromoCode code, long subtotalCents) {
    return discount(code.getDiscountType(), code.getAmount(), subtotalCents);
  }
}

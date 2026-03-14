package com.sorinwork.tickets.promo;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Works out how much a promo code takes off an order subtotal. */
public final class DiscountCalculator {

  private static final BigDecimal HUNDRED = BigDecimal.valueOf(100);

  private DiscountCalculator() {}

  /**
   * Percentages are rounded to the nearest cent (half up), so 15% of 59.97 is 9.00.
   *
   * @return the discount in cents, never more than the subtotal
   */
  public static long discount(DiscountType type, long amount, long subtotalCents) {
    if (subtotalCents <= 0) {
      return 0;
    }
    long discount =
        switch (type) {
          case PERCENT ->
              BigDecimal.valueOf(subtotalCents)
                  .multiply(BigDecimal.valueOf(Math.min(amount, 100)))
                  .divide(HUNDRED, 0, RoundingMode.HALF_UP)
                  .longValueExact();
          case FIXED -> amount;
        };
    return Math.min(discount, subtotalCents);
  }

  public static long discount(PromoCode code, long subtotalCents) {
    return discount(code.getDiscountType(), code.getAmount(), subtotalCents);
  }
}

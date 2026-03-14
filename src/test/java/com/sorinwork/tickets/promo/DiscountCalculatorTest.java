package com.sorinwork.tickets.promo;

import static com.sorinwork.tickets.promo.DiscountCalculator.discount;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DiscountCalculatorTest {

  @Test
  void percentageOfSubtotal() {
    assertThat(discount(DiscountType.PERCENT, 10, 5000)).isEqualTo(500);
    assertThat(discount(DiscountType.PERCENT, 25, 4000)).isEqualTo(1000);
  }

  @Test
  void percentageIsRoundedToTheNearestCent() {
    // 15% of 59.97 is 8.9955
    assertThat(discount(DiscountType.PERCENT, 15, 5997)).isEqualTo(900);
    // 10% of 0.05 is half a cent
    assertThat(discount(DiscountType.PERCENT, 10, 5)).isEqualTo(1);
    // 33% of 10.01 is 3.3033
    assertThat(discount(DiscountType.PERCENT, 33, 1001)).isEqualTo(330);
  }

  @Test
  void hundredPercentMakesItFree() {
    assertThat(discount(DiscountType.PERCENT, 100, 1999)).isEqualTo(1999);
  }

  @Test
  void fixedAmount() {
    assertThat(discount(DiscountType.FIXED, 500, 3000)).isEqualTo(500);
  }

  @Test
  void neverMoreThanTheSubtotal() {
    assertThat(discount(DiscountType.FIXED, 5000, 3000)).isEqualTo(3000);
    assertThat(discount(DiscountType.PERCENT, 150, 3000)).isEqualTo(3000);
  }

  @Test
  void nothingOffFreeOrders() {
    assertThat(discount(DiscountType.FIXED, 500, 0)).isZero();
  }
}

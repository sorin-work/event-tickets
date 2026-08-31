package com.sorinwork.tickets.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.promo.DiscountType;
import com.sorinwork.tickets.promo.PromoForm;
import com.sorinwork.tickets.support.TestData;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

class FormValidationTest {

  static jakarta.validation.ValidatorFactory factory;
  static Validator validator;

  @BeforeAll
  static void setUp() {
    factory = Validation.buildDefaultValidatorFactory();
    validator = factory.getValidator();
  }

  @AfterAll
  static void tearDown() {
    factory.close();
  }

  private static <T> Set<String> invalid(T form) {
    return validator.validate(form).stream()
        .map(ConstraintViolation::getPropertyPath)
        .map(Object::toString)
        .collect(Collectors.toSet());
  }

  @Test
  void aCompleteEventFormIsValid() {
    assertThat(invalid(TestData.eventForm("Jazz"))).isEmpty();
  }

  @Test
  void eventNeedsTitleVenueAndStart() {
    EventForm form = new EventForm();

    assertThat(invalid(form)).contains("title", "venueName", "startsAt");
  }

  @Test
  void eventCapacityMustBePositive() {
    EventForm form = TestData.eventForm("Jazz");
    form.setCapacity(0);

    assertThat(invalid(form)).containsExactly("capacity");
  }

  @Test
  void tierFormChecksPriceAndQuantity() {
    TierForm form = TestData.tierForm("Standing", "-5", -1);

    assertThat(invalid(form)).contains("priceValid", "quantity");
  }

  @Test
  void tierMaxPerOrderHasLimits() {
    TierForm form = TestData.tierForm("Standing", "10", 10);
    form.setMaxPerOrder(51);

    assertThat(invalid(form)).containsExactly("maxPerOrder");
  }

  @Test
  void salesCannotEndTheMomentTheyStart() {
    TierForm form = TestData.tierForm("Standing", "10", 10);
    form.setSalesStart(LocalDateTime.of(2030, 1, 1, 10, 0));
    form.setSalesEnd(LocalDateTime.of(2030, 1, 1, 10, 0));

    assertThat(invalid(form)).containsExactly("salesWindowValid");
  }

  @Test
  void promoCodesUseSafeCharacters() {
    PromoForm form = new PromoForm();
    form.setCode("no spaces!");
    form.setDiscountType(DiscountType.FIXED);
    form.setAmount("5");

    assertThat(invalid(form)).containsExactly("code");
  }

  @Test
  void fixedPromoAmountsAreMoney() {
    PromoForm form = new PromoForm();
    form.setCode("FIVE");
    form.setDiscountType(DiscountType.FIXED);
    form.setAmount("five");

    assertThat(invalid(form)).containsExactly("amountValid");
  }
}

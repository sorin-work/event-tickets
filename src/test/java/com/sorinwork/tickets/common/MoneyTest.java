package com.sorinwork.tickets.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MoneyTest {

  @ParameterizedTest
  @CsvSource({"12,1200", "12.5,1250", "12.50,1250", "'12,05',1205", "0,0", " 7.99 ,799"})
  void parsesAmounts(String text, long cents) {
    assertThat(Money.parse(text)).isEqualTo(cents);
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "abc", "-1", "1.999", "12.5.0"})
  void rejectsInvalidAmounts(String text) {
    assertThatThrownBy(() -> Money.parse(text)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void formatsKnownCurrenciesWithSymbol() {
    assertThat(Money.format(125000, "EUR")).isEqualTo("€1,250.00");
    assertThat(Money.format(999, "GBP")).isEqualTo("£9.99");
  }

  @Test
  void formatsOtherCurrenciesWithCode() {
    assertThat(Money.format(4500, "RON")).isEqualTo("45.00 RON");
  }

  @Test
  void formatsNegativeAmounts() {
    assertThat(Money.format(-250, "EUR")).isEqualTo("-€2.50");
  }

  @Test
  void convertsToDecimalText() {
    assertThat(Money.toDecimal(1250)).isEqualTo("12.50");
    assertThat(Money.toDecimal(5)).isEqualTo("0.05");
  }
}

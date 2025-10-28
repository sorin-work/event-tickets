package com.sorinwork.tickets.common;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;

/** Amounts are kept as whole cents ({@code long}) everywhere; this converts them for people. */
public final class Money {

  private static final Map<String, String> SYMBOLS = Map.of("EUR", "€", "USD", "$", "GBP", "£");

  private Money() {}

  /**
   * Parses an amount typed by a person, e.g. {@code "12"}, {@code "12.5"} or {@code "12,50"}.
   *
   * @throws IllegalArgumentException when the text is not a non-negative amount with at most two
   *     decimals
   */
  public static long parse(String text) {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("Amount is required");
    }
    String normalized = text.trim().replace(',', '.');
    BigDecimal value;
    try {
      value = new BigDecimal(normalized);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException("Not an amount: " + text);
    }
    if (value.signum() < 0) {
      throw new IllegalArgumentException("Amount cannot be negative");
    }
    if (value.stripTrailingZeros().scale() > 2) {
      throw new IllegalArgumentException("Use at most two decimals");
    }
    return value.movePointRight(2).setScale(0, RoundingMode.UNNECESSARY).longValueExact();
  }

  /** Plain decimal form for form fields, e.g. {@code 1250 -> "12.50"}. */
  public static String toDecimal(long cents) {
    return BigDecimal.valueOf(cents, 2).toPlainString();
  }

  /** Display form, e.g. {@code €1,250.00} or {@code 45.00 RON}. */
  public static String format(long cents, String currency) {
    DecimalFormat format =
        new DecimalFormat("#,##0.00", DecimalFormatSymbols.getInstance(Locale.UK));
    String amount = format.format(BigDecimal.valueOf(cents, 2));
    if (cents < 0) {
      amount = amount.substring(1);
    }
    String sign = cents < 0 ? "-" : "";
    String symbol = SYMBOLS.get(currency);
    return symbol != null ? sign + symbol + amount : sign + amount + " " + currency;
  }
}

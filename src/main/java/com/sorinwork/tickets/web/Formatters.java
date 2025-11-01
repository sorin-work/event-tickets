package com.sorinwork.tickets.web;

import com.sorinwork.tickets.common.Money;
import org.springframework.stereotype.Component;

/** Helpers for templates, available as {@code ${@fmt.money(...)}}. */
@Component("fmt")
public class Formatters {

  public String money(long cents, String currency) {
    return Money.format(cents, currency);
  }

  public String price(long cents, String currency) {
    return cents == 0 ? "Free" : Money.format(cents, currency);
  }
}

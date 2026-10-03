package com.sorinwork.tickets.web;

import com.sorinwork.tickets.config.AppProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every page template can use. */
@ControllerAdvice
public class GlobalModel {

  private final AppProperties properties;

  public GlobalModel(AppProperties properties) {
    this.properties = properties;
  }

  @ModelAttribute("organizerEmail")
  String organizerEmail() {
    String email = properties.organizerEmail();
    return email == null || email.isBlank() ? null : email.trim();
  }
}

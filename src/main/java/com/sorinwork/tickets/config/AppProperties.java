package com.sorinwork.tickets.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(@DefaultValue Admin admin, @DefaultValue Checkout checkout) {

  /** Credentials for the first admin account, created on startup when no admin exists. */
  public record Admin(String email, String password, @DefaultValue("Admin") String name) {}

  /** How long seats stay reserved while a buyer fills in the checkout form. */
  public record Checkout(@DefaultValue("10m") Duration holdDuration) {}
}

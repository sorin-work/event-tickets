package com.sorinwork.tickets.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
    @DefaultValue("http://localhost:8080") String baseUrl,
    @DefaultValue("") String organizerEmail,
    @DefaultValue Admin admin,
    @DefaultValue Checkout checkout,
    @DefaultValue Waitlist waitlist) {

  /** Absolute link to a path of the app, for emails. */
  public String url(String path) {
    String base = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
    return base + path;
  }

  /** Credentials for the first admin account, created on startup when no admin exists. */
  public record Admin(String email, String password, @DefaultValue("Admin") String name) {}

  /**
   * How long seats stay reserved while a buyer fills in the checkout form, how many seats one
   * visitor may hold in open checkouts of one event, and how many checkouts one visitor may start
   * per minute. {@code 0} turns either limit off.
   */
  public record Checkout(
      @DefaultValue("10m") Duration holdDuration,
      @DefaultValue("12") int maxHeldSeats,
      @DefaultValue("20") int startsPerMinute) {}

  /** How long released tickets stay reserved for the person they were offered to. */
  public record Waitlist(@DefaultValue("24h") Duration offerDuration) {}
}

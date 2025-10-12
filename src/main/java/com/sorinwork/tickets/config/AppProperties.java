package com.sorinwork.tickets.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

@ConfigurationProperties(prefix = "app")
public record AppProperties(@DefaultValue Admin admin) {

  /** Credentials for the first admin account, created on startup when no admin exists. */
  public record Admin(String email, String password, @DefaultValue("Admin") String name) {}
}

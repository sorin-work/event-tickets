package com.sorinwork.tickets.web;

import com.sorinwork.tickets.config.AppProperties;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.info.BuildProperties;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

/** Values every page template can use. */
@ControllerAdvice
public class GlobalModel {

  private final AppProperties properties;
  private final String version;

  public GlobalModel(AppProperties properties, ObjectProvider<BuildProperties> build) {
    this.properties = properties;
    BuildProperties info = build.getIfAvailable();
    this.version = info == null ? "dev" : info.getVersion();
  }

  @ModelAttribute("appVersion")
  String appVersion() {
    return version;
  }

  @ModelAttribute("organizerEmail")
  String organizerEmail() {
    String email = properties.organizerEmail();
    return email == null || email.isBlank() ? null : email.trim();
  }
}

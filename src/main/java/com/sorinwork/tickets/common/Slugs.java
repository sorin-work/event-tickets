package com.sorinwork.tickets.common;

import java.util.Locale;

/** Builds URL-friendly identifiers such as {@code summer-jazz-night} from titles. */
public final class Slugs {

  public static final int MAX_LENGTH = 60;

  private Slugs() {}

  public static String slugify(String text) {
    if (text == null) {
      return "";
    }
    String slug =
        text.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    if (slug.length() > MAX_LENGTH) {
      slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
    }
    return slug;
  }
}

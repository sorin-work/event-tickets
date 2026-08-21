package com.sorinwork.tickets.common;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Map;

/** Builds URL-friendly identifiers such as {@code summer-jazz-night} from titles. */
public final class Slugs {

  public static final int MAX_LENGTH = 60;

  /** Used when a title has no letters we can put in a URL, e.g. one written in Cyrillic. */
  public static final String FALLBACK = "event";

  /** Letters that do not decompose into a base letter plus accents. */
  private static final Map<String, String> SPECIAL =
      Map.of("ß", "ss", "æ", "ae", "œ", "oe", "ø", "o", "ł", "l", "đ", "d", "þ", "th");

  private Slugs() {}

  public static String slugify(String text) {
    if (text == null) {
      return FALLBACK;
    }
    String ascii =
        Normalizer.normalize(text.toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
            .replaceAll("\\p{M}+", "");
    for (Map.Entry<String, String> special : SPECIAL.entrySet()) {
      ascii = ascii.replace(special.getKey(), special.getValue());
    }
    String slug = ascii.replaceAll("[^a-z0-9]+", "-").replaceAll("^-+|-+$", "");
    if (slug.isEmpty()) {
      return FALLBACK;
    }
    if (slug.length() > MAX_LENGTH) {
      slug = slug.substring(0, MAX_LENGTH).replaceAll("-+$", "");
    }
    return slug;
  }
}

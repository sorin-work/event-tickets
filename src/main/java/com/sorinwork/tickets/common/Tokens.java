package com.sorinwork.tickets.common;

import java.security.SecureRandom;
import java.util.Base64;

/** Random identifiers for order references, buyer links and ticket codes. */
public final class Tokens {

  /** No 0/O, 1/I/L so codes can be read out loud and typed from paper. */
  static final String ALPHABET = "23456789ABCDEFGHJKMNPQRSTUVWXYZ";

  private static final SecureRandom RANDOM = new SecureRandom();

  private Tokens() {}

  /** E.g. {@code ET-7K3F9Q}. */
  public static String orderReference() {
    return "ET-" + code(6);
  }

  /** Unguessable token for links sent to buyers. */
  public static String accessToken() {
    byte[] bytes = new byte[24];
    RANDOM.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /** Code printed on a ticket and encoded in its QR code. */
  public static String ticketCode() {
    return code(12);
  }

  static String code(int length) {
    StringBuilder sb = new StringBuilder(length);
    for (int i = 0; i < length; i++) {
      sb.append(ALPHABET.charAt(RANDOM.nextInt(ALPHABET.length())));
    }
    return sb.toString();
  }
}

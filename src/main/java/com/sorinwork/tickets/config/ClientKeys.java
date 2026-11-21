package com.sorinwork.tickets.config;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Turns a client address into a key that identifies the visitor for a while without storing the
 * address itself. The secret is random per process: keys only need to stay the same for as long as
 * a hold lasts, and a restart simply starts counting again.
 */
@Component
public class ClientKeys {

  private final SecretKeySpec secret;

  public ClientKeys() {
    byte[] bytes = new byte[32];
    new SecureRandom().nextBytes(bytes);
    this.secret = new SecretKeySpec(bytes, "HmacSHA256");
  }

  /** 64 hex characters, or null when there is no address. */
  public String of(String address) {
    if (address == null || address.isBlank()) {
      return null;
    }
    try {
      Mac mac = Mac.getInstance("HmacSHA256");
      mac.init(secret);
      return HexFormat.of().formatHex(mac.doFinal(address.getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA256 is not available", e);
    }
  }
}

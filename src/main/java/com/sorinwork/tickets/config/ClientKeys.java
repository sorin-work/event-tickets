package com.sorinwork.tickets.config;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.regex.Pattern;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Component;

/**
 * Turns a client address into a key that identifies the visitor for a while without storing the
 * address itself. The secret is random per process: keys only need to stay the same for as long as
 * a hold lasts, and a restart simply starts counting again.
 *
 * <p>IPv6 addresses are keyed by their /64, the block one household or phone usually gets, so a
 * visitor cannot pass the limits by picking another address from it. IPv4 addresses mapped into
 * IPv6 ({@code ::ffff:203.0.113.7}) count as the IPv4 address.
 */
@Component
public class ClientKeys {

  /** Only IP literals are parsed, so looking up a key never triggers a DNS lookup. */
  private static final Pattern IP_LITERAL = Pattern.compile("[0-9A-Fa-f:.]+");

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
      return HexFormat.of().formatHex(mac.doFinal(normalize(address)));
    } catch (GeneralSecurityException e) {
      throw new IllegalStateException("HmacSHA256 is not available", e);
    }
  }

  /** The bytes that identify the visitor: the IPv4 address, or the first 64 bits of an IPv6 one. */
  static byte[] normalize(String address) {
    String literal = address.trim();
    int zone = literal.indexOf('%');
    if (zone >= 0) {
      literal = literal.substring(0, zone);
    }
    if (literal.startsWith("[") && literal.endsWith("]")) {
      literal = literal.substring(1, literal.length() - 1);
    }
    if (IP_LITERAL.matcher(literal).matches()) {
      try {
        // A literal is parsed, not resolved. Java returns mapped addresses as Inet4Address.
        InetAddress parsed = InetAddress.getByName(literal);
        byte[] bytes = parsed.getAddress();
        return parsed instanceof Inet6Address ? Arrays.copyOf(bytes, 8) : bytes;
      } catch (UnknownHostException e) {
        // Not a valid address after all; fall through and key the text as it is.
      }
    }
    return address.getBytes(StandardCharsets.UTF_8);
  }
}

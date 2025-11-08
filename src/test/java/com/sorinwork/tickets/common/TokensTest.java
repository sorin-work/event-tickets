package com.sorinwork.tickets.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TokensTest {

  @Test
  void orderReferencesHaveAPrefixAndSixCharacters() {
    assertThat(Tokens.orderReference()).matches("ET-[" + Tokens.ALPHABET + "]{6}");
  }

  @Test
  void ticketCodesAvoidAmbiguousCharacters() {
    for (int i = 0; i < 200; i++) {
      assertThat(Tokens.ticketCode()).hasSize(12).doesNotContainPattern("[01ILO]");
    }
  }

  @Test
  void accessTokensAreUrlSafeAndUnique() {
    Set<String> seen = new HashSet<>();
    for (int i = 0; i < 500; i++) {
      String token = Tokens.accessToken();
      assertThat(token).hasSize(32).matches("[A-Za-z0-9_-]+");
      assertThat(seen.add(token)).isTrue();
    }
  }
}

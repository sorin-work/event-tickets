package com.sorinwork.tickets.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SlugsTest {

  @Test
  void lowercasesAndJoinsWordsWithDashes() {
    assertThat(Slugs.slugify("Summer Jazz Night")).isEqualTo("summer-jazz-night");
  }

  @Test
  void collapsesPunctuationAndSpaces() {
    assertThat(Slugs.slugify("  Rust & Coffee:   Meetup #12!! "))
        .isEqualTo("rust-coffee-meetup-12");
  }

  @Test
  void handlesNull() {
    assertThat(Slugs.slugify(null)).isEqualTo("event");
  }

  @Test
  void keepsAccentedLettersAsPlainOnes() {
    assertThat(Slugs.slugify("Café Müller Night")).isEqualTo("cafe-muller-night");
    assertThat(Slugs.slugify("Șezătoare în Grădină")).isEqualTo("sezatoare-in-gradina");
    assertThat(Slugs.slugify("Straße Fest Ølstue")).isEqualTo("strasse-fest-olstue");
  }

  @Test
  void titlesWithoutLatinLettersGetAFallback() {
    assertThat(Slugs.slugify("Концерт")).isEqualTo("event");
    assertThat(Slugs.slugify("!!!")).isEqualTo("event");
    assertThat(Slugs.slugify("Концерт 2030")).isEqualTo("2030");
  }

  @Test
  void limitsLengthWithoutTrailingDash() {
    String slug = Slugs.slugify("a".repeat(59) + " bcd");

    assertThat(slug).hasSizeLessThanOrEqualTo(Slugs.MAX_LENGTH).doesNotEndWith("-");
  }
}

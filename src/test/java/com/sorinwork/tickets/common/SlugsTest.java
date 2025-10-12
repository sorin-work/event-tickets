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
    assertThat(Slugs.slugify(null)).isEmpty();
  }

  @Test
  void limitsLengthWithoutTrailingDash() {
    String slug = Slugs.slugify("a".repeat(59) + " bcd");

    assertThat(slug).hasSizeLessThanOrEqualTo(Slugs.MAX_LENGTH).doesNotEndWith("-");
  }
}

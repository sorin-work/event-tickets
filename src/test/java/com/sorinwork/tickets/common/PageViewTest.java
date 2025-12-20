package com.sorinwork.tickets.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class PageViewTest {

  private static PageView<Integer> page(int page, int size, long total, int itemsOnPage) {
    return new PageView<>(Collections.nCopies(itemsOnPage, 1), page, size, total);
  }

  @Test
  void singlePage() {
    PageView<Integer> view = page(1, 20, 20, 20);

    assertThat(view.totalPages()).isEqualTo(1);
    assertThat(view.hasNext()).isFalse();
    assertThat(view.hasPrevious()).isFalse();
  }

  @Test
  void middlePage() {
    PageView<Integer> view = page(2, 20, 60, 20);

    assertThat(view.totalPages()).isEqualTo(3);
    assertThat(view.hasNext()).isTrue();
    assertThat(view.hasPrevious()).isTrue();
    assertThat(view.firstItem()).isEqualTo(21);
    assertThat(view.lastItem()).isEqualTo(40);
  }

  @Test
  void emptyResultStillHasOnePage() {
    PageView<Integer> view = new PageView<>(List.of(), 1, 20, 0);

    assertThat(view.totalPages()).isEqualTo(1);
    assertThat(view.isEmpty()).isTrue();
    assertThat(view.firstItem()).isZero();
  }
}

package com.sorinwork.tickets.common;

import java.util.List;
import org.springframework.data.domain.Page;

/** One page of results for templates, with 1-based page numbers. */
public record PageView<T>(List<T> items, int page, int size, long total) {

  public static <T> PageView<T> of(Page<T> page) {
    return new PageView<>(
        page.getContent(), page.getNumber() + 1, page.getSize(), page.getTotalElements());
  }

  public int totalPages() {
    return Math.max(1, (int) (total / size));
  }

  public boolean hasPrevious() {
    return page > 1;
  }

  public boolean hasNext() {
    return page < totalPages();
  }

  public boolean isEmpty() {
    return items.isEmpty();
  }

  /** Position of the first item on this page, for "showing 21-40 of 95". */
  public long firstItem() {
    return items.isEmpty() ? 0 : (long) (page - 1) * size + 1;
  }

  public long lastItem() {
    return items.isEmpty() ? 0 : firstItem() + items.size() - 1;
  }
}

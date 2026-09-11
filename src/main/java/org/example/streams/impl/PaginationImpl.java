package org.example.streams.impl;

import java.util.List;

public class PaginationImpl {

  public List<String> getPageSortedAsc(List<String> items, int page, int pageSize) {
    final int totalItems = items.size();
    final int pages = totalItems / pageSize;

    if (page <= 0 || page > pages) {
      throw new IllegalArgumentException("Page " + page + " does not exist. Total pages: " + pages);
    }

    final long computedJump = (long) (page - 1) * pageSize;

    return items.stream().sorted().skip(computedJump).limit(pageSize).toList();
  }
}

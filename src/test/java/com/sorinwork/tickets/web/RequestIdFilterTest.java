package com.sorinwork.tickets.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.sorinwork.tickets.support.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;

class RequestIdFilterTest extends IntegrationTest {

  @Test
  void addsARequestId() throws Exception {
    mvc.perform(get("/"))
        .andExpect(header().string("X-Request-Id", Matchers.matchesPattern("[0-9a-f]{8}")));
  }

  @Test
  void keepsTheIdFromAProxy() throws Exception {
    mvc.perform(get("/").header("X-Request-Id", "abc-123"))
        .andExpect(header().string("X-Request-Id", "abc-123"));
  }

  @Test
  void ignoresUnsafeIds() throws Exception {
    mvc.perform(get("/").header("X-Request-Id", "evil\nheader"))
        .andExpect(header().string("X-Request-Id", Matchers.matchesPattern("[0-9a-f]{8}")));
  }
}

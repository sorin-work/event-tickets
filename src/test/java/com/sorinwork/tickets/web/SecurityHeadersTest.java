package com.sorinwork.tickets.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;

import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class SecurityHeadersTest extends IntegrationTest {

  @Test
  void pagesSendSecurityHeaders() throws Exception {
    mvc.perform(get("/"))
        .andExpect(
            header().string("Content-Security-Policy", containsString("frame-ancestors 'none'")))
        .andExpect(header().string("X-Content-Type-Options", "nosniff"))
        .andExpect(header().string("Referrer-Policy", "same-origin"))
        .andExpect(header().string("Permissions-Policy", containsString("camera=(self)")));
  }
}

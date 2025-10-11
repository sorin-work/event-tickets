package com.sorinwork.tickets.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class HomeControllerTest extends IntegrationTest {

  @Test
  void rendersHomePage() throws Exception {
    mvc.perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Upcoming events")));
  }
}

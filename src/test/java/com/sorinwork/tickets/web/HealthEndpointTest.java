package com.sorinwork.tickets.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;

class HealthEndpointTest extends IntegrationTest {

  @Test
  void healthIsUp() throws Exception {
    mvc.perform(get("/actuator/health"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("UP"));
  }

  @Test
  void infoShowsTheVersion() throws Exception {
    mvc.perform(get("/actuator/info"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.build.artifact").value("event-tickets"));
  }
}

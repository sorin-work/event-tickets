package com.sorinwork.tickets.admin;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AdminSecurityTest extends IntegrationTest {

  @ParameterizedTest
  @ValueSource(
      strings = {
        "/admin/events",
        "/admin/events/1/attendees",
        "/admin/events/1/attendees.csv",
        "/admin/events/1/orders.csv",
        "/admin/events/1/report.csv",
        "/admin/orders/1",
        "/admin/outbox",
        "/admin/users"
      })
  void adminPagesNeedASignedInAdmin(String url) throws Exception {
    mvc.perform(get(url))
        .andExpect(status().is3xxRedirection())
        .andExpect(redirectedUrlPattern("**/admin/login"));
  }

  @Test
  void formsNeedACsrfToken() throws Exception {
    mvc.perform(post("/admin/events").with(user("admin")).param("title", "X"))
        .andExpect(status().isForbidden());
    mvc.perform(post("/admin/orders/1/cancel").with(user("admin")))
        .andExpect(status().isForbidden());
  }

  @Test
  void signedOutPostsGoToTheLoginPage() throws Exception {
    mvc.perform(post("/admin/orders/1/cancel").with(csrf()))
        .andExpect(redirectedUrlPattern("**/admin/login"));
  }

  @Test
  void publicPagesAreOpen() throws Exception {
    mvc.perform(get("/")).andExpect(status().isOk());
    mvc.perform(get("/events/past")).andExpect(status().isOk());
    mvc.perform(get("/tickets/find")).andExpect(status().isOk());
  }
}

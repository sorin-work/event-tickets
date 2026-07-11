package com.sorinwork.tickets.admin;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.notify.Notifier;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminOutboxControllerTest extends IntegrationTest {

  @Autowired Notifier notifier;

  @Test
  void showsSentEmails() throws Exception {
    notifier.send("ana@example.com", "Your tickets", "Hello <b>Ana</b>", null);

    mvc.perform(get("/admin/outbox"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("Your tickets")))
        .andExpect(content().string(containsString("to ana@example.com")))
        .andExpect(content().string(containsString("Hello &lt;b&gt;Ana&lt;/b&gt;")))
        .andExpect(content().string(not(containsString("<b>Ana</b>"))));
  }

  @Test
  void searchesByRecipient() throws Exception {
    notifier.send("ana@example.com", "For Ana", "Body", null);
    notifier.send("bob@example.com", "For Bob", "Body", null);

    mvc.perform(get("/admin/outbox").param("to", "ANA@"))
        .andExpect(content().string(containsString("For Ana")))
        .andExpect(content().string(not(containsString("For Bob"))));
  }

  @Test
  void emptyState() throws Exception {
    mvc.perform(get("/admin/outbox"))
        .andExpect(content().string(containsString("No emails sent yet.")));
  }
}

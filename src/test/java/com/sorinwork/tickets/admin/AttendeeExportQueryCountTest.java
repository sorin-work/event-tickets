package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.SessionFactory;
import org.hibernate.stat.Statistics;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AttendeeExportQueryCountTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired EntityManagerFactory entityManagerFactory;

  @Test
  void exportUsesAFixedNumberOfQueries() throws Exception {
    Event event = data.event("Big Conference");
    TicketTier day = data.tier(event, "Day pass", "50", 500);
    TicketTier vip = data.tier(event, "VIP", "150", 50);
    events.publish(event.getId());
    for (int i = 0; i < 20; i++) {
      purchases.buy(i % 4 == 0 ? vip : day, 1, "Person " + i, "p" + i + "@example.com");
    }
    Statistics statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.clear();

    String csv =
        mvc.perform(get("/admin/events/" + event.getId() + "/attendees.csv"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    assertThat(csv.split("\r\n")).hasSize(21);
    // loading the event and one query for all tickets with their tier and order
    assertThat(statistics.getPrepareStatementCount()).isLessThanOrEqualTo(3);
  }
}

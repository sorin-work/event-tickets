package com.sorinwork.tickets.admin;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminReportControllerTest extends IntegrationTest {

  @Autowired EventService events;

  @Test
  void showsTotalsAndTiers() throws Exception {
    Event event = data.event("Rock Night");
    TicketTier tier = data.tier(event, "Standing", "20", 100);
    events.publish(event.getId());
    purchases.buy(tier, 3, "Ana", "ana@example.com");

    mvc.perform(get("/admin/events/" + event.getId() + "/report"))
        .andExpect(status().isOk())
        .andExpect(content().string(containsString("3 / 100")))
        .andExpect(content().string(containsString("€60.00")))
        .andExpect(content().string(containsString("width:3%")));
  }

  @Test
  void exportsTheReportAsCsv() throws Exception {
    Event event = data.event("Rock Night");
    TicketTier tier = data.tier(event, "Standing", "20", 100);
    events.publish(event.getId());
    purchases.buy(tier, 3, "Ana", "ana@example.com");

    String csv =
        mvc.perform(get("/admin/events/" + event.getId() + "/report.csv"))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsString();

    org.assertj.core.api.Assertions.assertThat(csv)
        .startsWith("Section,Name,Price,Sold,Capacity,Amount (EUR)\r\n")
        .contains("Tier,Standing,20.00,3,100,60.00\r\n")
        .contains("Total,Net,,,,60.00\r\n")
        .contains("Day,2030-01-15,,3,,60.00\r\n");
  }

  @Test
  void rendersEventsWithoutSales() throws Exception {
    Event event = data.event("Quiet Night");
    data.tier(event, "Entry", "5", 10);

    mvc.perform(get("/admin/events/" + event.getId() + "/report"))
        .andExpect(content().string(containsString("0 / 10")));
  }
}

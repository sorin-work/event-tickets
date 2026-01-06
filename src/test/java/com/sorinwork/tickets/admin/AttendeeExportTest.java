package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AttendeeExportTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired OrderAdminService orders;

  @Test
  void exportsValidTicketsAsCsv() throws Exception {
    Event event = data.event("Book Club");
    TicketTier tier = data.tier(event, "Member", "0", 30);
    events.publish(event.getId());
    Order order = purchases.buy(tier, 2, "Pop, Ana", "ana@example.com");
    Order cancelled = purchases.buy(tier, 1, "Gone", "gone@example.com");
    orders.cancel(cancelled.getId());

    String csv =
        mvc.perform(get("/admin/events/" + event.getId() + "/attendees.csv"))
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
            .andExpect(
                header()
                    .string(
                        "Content-Disposition",
                        org.hamcrest.Matchers.containsString("book-club-attendees.csv")))
            .andReturn()
            .getResponse()
            .getContentAsString();

    String[] lines = csv.split("\r\n");
    assertThat(lines).hasSize(3);
    assertThat(lines[0]).isEqualTo("Name,Email,Tier,Ticket code,Order,Checked in");
    assertThat(lines[1])
        .startsWith("\"Pop, Ana\",ana@example.com,Member,")
        .endsWith(order.getReference() + ",");
    assertThat(csv).doesNotContain("Gone");
  }
}

package com.sorinwork.tickets.web;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.ticket.Ticket;
import com.sorinwork.tickets.ticket.TicketRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;

class TicketQrCodeTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired TicketRepository tickets;
  @Autowired com.sorinwork.tickets.order.OrderAdminService orderAdmin;
  @Autowired com.sorinwork.tickets.ticket.CheckInService checkIn;

  Order mine;
  Order theirs;
  Ticket myTicket;
  Ticket theirTicket;

  @BeforeEach
  void setUp() {
    Event event = data.event("Concert");
    TicketTier tier = data.tier(event, "Seat", "20", 50);
    events.publish(event.getId());
    mine = purchases.buy(tier, 1, "Me", "me@example.com");
    theirs = purchases.buy(tier, 1, "Someone Else", "else@example.com");
    myTicket = tickets.findByOrderIdOrderById(mine.getId()).get(0);
    theirTicket = tickets.findByOrderIdOrderById(theirs.getId()).get(0);
  }

  @Test
  void servesTheQrCodeUnderTheOrderLink() throws Exception {
    mvc.perform(
            get("/orders/" + mine.getAccessToken() + "/tickets/" + myTicket.getId() + "/qr.png"))
        .andExpect(status().isOk())
        .andExpect(content().contentType(MediaType.IMAGE_PNG));
  }

  @Test
  void anotherBuyersTicketCannotBeFetchedWithMyLink() throws Exception {
    mvc.perform(
            get("/orders/" + mine.getAccessToken() + "/tickets/" + theirTicket.getId() + "/qr.png"))
        .andExpect(status().isNotFound());
  }

  @Test
  void ticketIdsAloneDoNotGiveAccess() throws Exception {
    mvc.perform(get("/tickets/" + theirTicket.getId() + "/qr.png"))
        .andExpect(status().isNotFound());
  }

  @Test
  void cancelledOrdersSaySoAndShowNoQrCodes() throws Exception {
    orderAdmin.cancel(mine.getId());

    mvc.perform(get("/orders/" + mine.getAccessToken()))
        .andExpect(content().string(containsString("This order was cancelled")))
        .andExpect(content().string(org.hamcrest.Matchers.not(containsString("qr.png"))));
    mvc.perform(get("/checkout/" + mine.getAccessToken()))
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl(
                "/orders/" + mine.getAccessToken()));
  }

  @Test
  void showsWhenATicketWasUsed() throws Exception {
    checkIn.checkIn(mine.getEvent().getId(), myTicket.getCode(), "door");

    mvc.perform(get("/orders/" + mine.getAccessToken()))
        .andExpect(content().string(containsString("Checked in 12:00")));
  }

  @Test
  void orderPageUsesTheProtectedUrl() throws Exception {
    mvc.perform(get("/orders/" + mine.getAccessToken()))
        .andExpect(
            content()
                .string(
                    containsString(
                        "/orders/"
                            + mine.getAccessToken()
                            + "/tickets/"
                            + myTicket.getId()
                            + "/qr.png")));
  }
}

package com.sorinwork.tickets.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckInServiceTest extends IntegrationTest {

  @Autowired CheckInService checkIn;
  @Autowired EventService events;
  @Autowired OrderAdminService orders;
  @Autowired TicketRepository tickets;

  Event event;
  TicketTier tier;
  Order order;
  String code;

  @BeforeEach
  void setUp() {
    event = data.event("Tech Talk");
    tier = data.tier(event, "Seat", "0", 50);
    events.publish(event.getId());
    order = purchases.buy(tier, 1, "Ana Pop", "ana@example.com");
    code = tickets.findByOrderIdOrderById(order.getId()).get(0).getCode();
  }

  @Test
  void checksInAValidTicket() {
    CheckInResult result = checkIn.checkIn(event.getId(), code, "door@example.com");

    assertThat(result.status()).isEqualTo(CheckInResult.Status.CHECKED_IN);
    assertThat(result.admitted()).isTrue();
    Ticket ticket = tickets.findByCode(code).orElseThrow();
    assertThat(ticket.getCheckedInAt()).isEqualTo(clock.instant());
    assertThat(ticket.getCheckedInBy()).isEqualTo("door@example.com");
    assertThat(checkIn.checkedInCount(event.getId())).isEqualTo(1);
  }

  @Test
  void secondScanIsRejected() {
    checkIn.checkIn(event.getId(), code, "door");
    clock.advance(Duration.ofMinutes(3));

    CheckInResult again = checkIn.checkIn(event.getId(), code, "door");

    assertThat(again.status()).isEqualTo(CheckInResult.Status.ALREADY_CHECKED_IN);
    assertThat(again.ticket().getCheckedInAt())
        .isEqualTo(clock.instant().minus(Duration.ofMinutes(3)));
  }

  @Test
  void acceptsCodesTypedByHand() {
    String typed = code.substring(0, 4).toLowerCase() + " - " + code.substring(4);

    assertThat(checkIn.checkIn(event.getId(), typed, "door").admitted()).isTrue();
  }

  @Test
  void unknownCode() {
    assertThat(checkIn.checkIn(event.getId(), "NOPE", "door").status())
        .isEqualTo(CheckInResult.Status.NOT_FOUND);
    assertThat(checkIn.checkIn(event.getId(), null, "door").status())
        .isEqualTo(CheckInResult.Status.NOT_FOUND);
  }

  @Test
  void ticketForAnotherEvent() {
    Event other = data.event("Other Talk");

    assertThat(checkIn.checkIn(other.getId(), code, "door").status())
        .isEqualTo(CheckInResult.Status.WRONG_EVENT);
  }

  @Test
  void cancelledOrder() {
    orders.cancel(order.getId());

    assertThat(checkIn.checkIn(event.getId(), code, "door").status())
        .isEqualTo(CheckInResult.Status.CANCELLED);
    assertThat(tickets.findByCode(code).orElseThrow().isCheckedIn()).isFalse();
  }

  @Test
  void undoLetsTheTicketBeScannedAgain() {
    Ticket ticket = checkIn.checkIn(event.getId(), code, "door").ticket();

    checkIn.undo(ticket.getId());

    assertThat(checkIn.checkedInCount(event.getId())).isZero();
    assertThat(checkIn.checkIn(event.getId(), code, "door").admitted()).isTrue();
  }
}

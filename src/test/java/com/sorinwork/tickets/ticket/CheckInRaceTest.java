package com.sorinwork.tickets.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.support.PostgresTest;
import com.sorinwork.tickets.support.Race;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Several doors scanning the same ticket at the same moment, on PostgreSQL. */
class CheckInRaceTest extends PostgresTest {

  @Autowired CheckInService checkIn;
  @Autowired EventService events;
  @Autowired TicketRepository tickets;

  @Test
  void aTicketIsAdmittedOnce() throws Exception {
    Event event = data.event("Tech Talk");
    TicketTier tier = data.tier(event, "Seat", "0", 50);
    events.publish(event.getId());
    Order order = purchases.buy(tier, 1, "Ana Pop", "ana@example.com");
    String code = tickets.findByOrderIdOrderById(order.getId()).get(0).getCode();

    List<Callable<CheckInResult>> scans = new ArrayList<>();
    for (int i = 0; i < 10; i++) {
      String staff = "door " + i;
      scans.add(() -> checkIn.checkIn(event.getId(), code, staff));
    }

    List<Race.Outcome<CheckInResult>> outcomes = Race.run(scans);

    assertThat(outcomes).allSatisfy(o -> assertThat(o.succeeded()).isTrue());
    assertThat(outcomes.stream().filter(o -> o.value().admitted())).hasSize(1);
    assertThat(outcomes.stream().filter(o -> !o.value().admitted()))
        .allSatisfy(
            o -> assertThat(o.value().status()).isEqualTo(CheckInResult.Status.ALREADY_CHECKED_IN));
    assertThat(checkIn.checkedInCount(event.getId())).isEqualTo(1);
  }
}

package com.sorinwork.tickets.ticket;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.order.Order;
import com.sorinwork.tickets.order.OrderAdminService;
import com.sorinwork.tickets.order.OrderStatus;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CheckInServiceTest extends IntegrationTest {

  @Autowired CheckInService checkIn;
  @Autowired EventService events;
  @Autowired OrderAdminService orders;
  @Autowired TicketRepository tickets;
  @Autowired org.springframework.transaction.support.TransactionTemplate transactions;
  @Autowired org.springframework.jdbc.core.JdbcTemplate jdbc;

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

  @Test
  void theUpdateAdmitsATicketOnlyOnce() {
    long id = tickets.findByCode(code).orElseThrow().getId();

    assertThat(tickets.markCheckedIn(id, clock.instant(), "north door", OrderStatus.PAID))
        .isEqualTo(1);
    assertThat(tickets.markCheckedIn(id, clock.instant(), "south door", OrderStatus.PAID)).isZero();
    assertThat(tickets.findByCode(code).orElseThrow().getCheckedInBy()).isEqualTo("north door");
  }

  @Test
  void theUpdateRefusesATicketWhoseOrderWasCancelled() {
    long id = tickets.findByCode(code).orElseThrow().getId();
    orders.cancel(order.getId());

    assertThat(tickets.markCheckedIn(id, clock.instant(), "door", OrderStatus.PAID)).isZero();
    assertThat(tickets.findByCode(code).orElseThrow().isCheckedIn()).isFalse();
  }

  @Test
  void alreadyCheckedInNamesTheFirstScan() {
    checkIn.checkIn(event.getId(), code, "north door");

    CheckInResult again = checkIn.checkIn(event.getId(), code, "south door");

    assertThat(again.status()).isEqualTo(CheckInResult.Status.ALREADY_CHECKED_IN);
    assertThat(again.ticket().getCheckedInBy()).isEqualTo("north door");
  }

  @Test
  void undoOfATicketThatIsNotCheckedInChangesNothing() {
    long id = tickets.findByCode(code).orElseThrow().getId();

    Ticket ticket = checkIn.undo(id);

    assertThat(ticket.isCheckedIn()).isFalse();
    assertThat(checkIn.checkIn(event.getId(), code, "door").admitted()).isTrue();
  }

  @Test
  void scannersAtSeveralDoorsAdmitATicketOnce() throws Exception {
    int doors = 8;
    ExecutorService pool = Executors.newFixedThreadPool(doors);
    CountDownLatch start = new CountDownLatch(1);
    List<Future<CheckInResult>> scans = new ArrayList<>();
    try {
      for (int i = 0; i < doors; i++) {
        String staff = "door " + i;
        Callable<CheckInResult> scan =
            () -> {
              start.await();
              return checkIn.checkIn(event.getId(), code, staff);
            };
        scans.add(pool.submit(scan));
      }
      start.countDown();
      long admitted = 0;
      for (Future<CheckInResult> scan : scans) {
        CheckInResult result = scan.get(30, TimeUnit.SECONDS);
        if (result.admitted()) {
          admitted++;
        } else {
          assertThat(result.status()).isEqualTo(CheckInResult.Status.ALREADY_CHECKED_IN);
        }
      }
      assertThat(admitted).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
    assertThat(checkIn.checkedInCount(event.getId())).isEqualTo(1);
  }

  @Test
  void anOrderCancelledAfterTheTicketWasLoadedIsNotLetIn() {
    CheckInResult result =
        transactions.execute(
            status -> {
              // The ticket and its paid order are now in this transaction's persistence context.
              assertThat(tickets.findWithDetailsByCode(code).orElseThrow().getOrder().getStatus())
                  .isEqualTo(OrderStatus.PAID);
              // Cancelled behind its back, as a cancel committing at that moment would be.
              jdbc.update("update orders set status = 'CANCELLED' where id = ?", order.getId());
              return checkIn.checkIn(event.getId(), code, "door");
            });

    assertThat(result.status()).isEqualTo(CheckInResult.Status.CANCELLED);
    assertThat(tickets.findByCode(code).orElseThrow().isCheckedIn()).isFalse();
  }
}

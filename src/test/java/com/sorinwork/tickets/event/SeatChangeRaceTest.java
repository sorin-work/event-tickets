package com.sorinwork.tickets.event;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.order.AvailabilityService;
import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.order.TierAvailability;
import com.sorinwork.tickets.support.PostgresTest;
import com.sorinwork.tickets.support.Race;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * An admin lowering a tier or the venue while a buyer checks out, on PostgreSQL. One side takes the
 * event lock first and keeps it for a moment, so the other is certain to wait for it; each test
 * then knows exactly which side must win.
 */
class SeatChangeRaceTest extends PostgresTest {

  @Autowired EventService events;
  @Autowired EventRepository eventRows;
  @Autowired TierService tiers;
  @Autowired CheckoutService checkout;
  @Autowired AvailabilityService availability;
  @Autowired TransactionTemplate transactions;

  Event event;
  TicketTier tier;

  @BeforeEach
  void setUp() {
    event = data.event("Small Gig");
    tier = data.tier(event, "Floor", "15", 10);
    events.publish(event.getId());
    // Six of the ten seats are held already.
    checkout.start(event.getId(), Map.of(tier.getId(), 3));
    checkout.start(event.getId(), Map.of(tier.getId(), 3));
  }

  /**
   * Runs {@code first} in a transaction that takes the event lock, holds it a moment and then does
   * its work; {@code second} starts once the lock is taken and so has to wait for the commit.
   */
  private List<Race.Outcome<Object>> firstThenSecond(
      Supplier<Object> first, Supplier<Object> second) throws Exception {
    CountDownLatch locked = new CountDownLatch(1);
    Callable<Object> holder =
        () ->
            transactions.execute(
                status -> {
                  eventRows.lockById(event.getId());
                  locked.countDown();
                  sleep(300);
                  return first.get();
                });
    Callable<Object> waiter =
        () -> {
          assertThat(locked.await(30, TimeUnit.SECONDS)).isTrue();
          return second.get();
        };
    return Race.run(List.of(holder, waiter));
  }

  private static void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
    }
  }

  private Object lowerTierTo(int quantity) {
    TierForm form = TierForm.from(tiers.get(event.getId(), tier.getId()));
    form.setQuantity(quantity);
    return tiers.update(event.getId(), tier.getId(), form);
  }

  private Object lowerVenueTo(int capacity) {
    EventForm form = EventForm.from(events.get(event.getId()));
    form.setCapacity(capacity);
    return events.update(event.getId(), form);
  }

  private Object buy(int quantity) {
    return checkout.start(event.getId(), Map.of(tier.getId(), quantity));
  }

  private TierAvailability now() {
    return availability.forEvent(event.getId()).get(tier.getId());
  }

  @Test
  void aBuyerFirstMakesTheAdminsLowerQuantityTooLow() throws Exception {
    List<Race.Outcome<Object>> outcomes = firstThenSecond(() -> buy(2), () -> lowerTierTo(7));

    assertThat(outcomes.get(0).succeeded()).isTrue();
    assertThat(outcomes.get(1).error())
        .isInstanceOf(BusinessException.class)
        .hasMessageStartingWith("That is fewer than are taken: 0 sold and 8 held");
    assertThat(tiers.get(event.getId(), tier.getId()).getQuantity()).isEqualTo(10);
    assertThat(now().held()).isEqualTo(8);
  }

  @Test
  void anAdminFirstLeavesTheBuyerOnlyWhatIsLeft() throws Exception {
    List<Race.Outcome<Object>> outcomes = firstThenSecond(() -> lowerTierTo(7), () -> buy(2));

    assertThat(outcomes.get(0).succeeded()).isTrue();
    assertThat(outcomes.get(1).error())
        .isInstanceOf(BusinessException.class)
        .hasMessage("Only 1 Floor left.");
    assertThat(tiers.get(event.getId(), tier.getId()).getQuantity()).isEqualTo(7);
    assertThat(now().held()).isEqualTo(6);
  }

  @Test
  void aBuyerFirstMakesTheAdminsSmallerVenueTooSmall() throws Exception {
    List<Race.Outcome<Object>> outcomes = firstThenSecond(() -> buy(2), () -> lowerVenueTo(7));

    assertThat(outcomes.get(0).succeeded()).isTrue();
    assertThat(outcomes.get(1).error()).isInstanceOf(BusinessException.class);
    assertThat(events.get(event.getId()).getCapacity()).isNull();
  }

  @Test
  void anAdminFirstLeavesTheBuyerOnlyWhatTheVenueHasLeft() throws Exception {
    List<Race.Outcome<Object>> outcomes = firstThenSecond(() -> lowerVenueTo(7), () -> buy(2));

    assertThat(outcomes.get(0).succeeded()).isTrue();
    assertThat(outcomes.get(1).error()).hasMessage("Only 1 Floor left.");
    assertThat(events.get(event.getId()).getCapacity()).isEqualTo(7);
    assertThat(now().held()).isEqualTo(6);
  }
}

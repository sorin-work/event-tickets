package com.sorinwork.tickets.event;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sorinwork.tickets.order.CheckoutService;
import com.sorinwork.tickets.support.IntegrationTest;
import com.sorinwork.tickets.waitlist.WaitlistEntry;
import com.sorinwork.tickets.waitlist.WaitlistForm;
import com.sorinwork.tickets.waitlist.WaitlistRepository;
import com.sorinwork.tickets.waitlist.WaitlistService;
import com.sorinwork.tickets.waitlist.WaitlistStatus;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/** Changing how many tickets a tier has once some are sold or held. */
class TierQuantityTest extends IntegrationTest {

  @Autowired TierService tiers;
  @Autowired EventService events;
  @Autowired CheckoutService checkout;
  @Autowired WaitlistService waitlist;
  @Autowired WaitlistRepository waitlistEntries;

  Event event;
  TicketTier tier;

  @BeforeEach
  void setUp() {
    event = data.event("Small Gig");
    tier = data.tier(event, "Floor", "15", 5);
    event = events.publish(event.getId());
  }

  private TicketTier change(int quantity, boolean paused) {
    TierForm form = TierForm.from(tiers.get(event.getId(), tier.getId()));
    form.setQuantity(quantity);
    form.setPaused(paused);
    return tiers.update(event.getId(), tier.getId(), form);
  }

  private WaitlistEntry joinWaitlist() {
    return waitlist.join(
        events.get(event.getId()), new WaitlistForm(tier.getId(), "Dana", "dana@example.com", 1));
  }

  private WaitlistStatus statusOf(WaitlistEntry entry) {
    return waitlistEntries.findById(entry.getId()).orElseThrow().getStatus();
  }

  @Test
  void belowWhatIsSoldIsRefused() {
    purchases.buy(tier, 3, "Ana", "ana@example.com");

    assertThatThrownBy(() -> change(2, false)).hasMessage("That is fewer than are taken: 3 sold.");
    assertThat(tiers.get(event.getId(), tier.getId()).getQuantity()).isEqualTo(5);
  }

  @Test
  void belowSoldAndHeldIsRefused() {
    purchases.buy(tier, 2, "Ana", "ana@example.com");
    checkout.start(event.getId(), Map.of(tier.getId(), 2));

    assertThatThrownBy(() -> change(3, false))
        .hasMessage(
            "That is fewer than are taken: 2 sold and 2 held in open checkouts,"
                + " which end by 12:10.");
  }

  @Test
  void exactlyWhatIsTakenIsAccepted() {
    purchases.buy(tier, 2, "Ana", "ana@example.com");
    checkout.start(event.getId(), Map.of(tier.getId(), 2));

    assertThat(change(4, false).getQuantity()).isEqualTo(4);
  }

  @Test
  void moreTicketsForASoldOutTierGoToTheWaitlistFirst() {
    purchases.buy(tier, 5, "Ana", "ana@example.com");
    WaitlistEntry dana = joinWaitlist();

    change(6, false);

    assertThat(statusOf(dana)).isEqualTo(WaitlistStatus.OFFERED);
    assertThatThrownBy(() -> checkout.start(event.getId(), Map.of(tier.getId(), 1)))
        .hasMessage("Floor is sold out.");
  }

  @Test
  void noOfferWhileTheTierIsPausedButOneWhenItResumes() {
    purchases.buy(tier, 5, "Ana", "ana@example.com");
    WaitlistEntry dana = joinWaitlist();

    change(6, true);
    assertThat(statusOf(dana)).isEqualTo(WaitlistStatus.WAITING);

    change(6, false);
    assertThat(statusOf(dana)).isEqualTo(WaitlistStatus.OFFERED);
  }
}

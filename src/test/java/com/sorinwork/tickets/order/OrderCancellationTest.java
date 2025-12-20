package com.sorinwork.tickets.order;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.event.EventService;
import com.sorinwork.tickets.event.TicketTier;
import com.sorinwork.tickets.support.IntegrationTest;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class OrderCancellationTest extends IntegrationTest {

  @Autowired EventService events;
  @Autowired CheckoutService checkout;
  @Autowired OrderAdminService admin;
  @Autowired AvailabilityService availability;

  Event event;
  TicketTier tier;

  @BeforeEach
  void setUp() {
    event = data.event("Choir Concert");
    tier = data.tier(event, "Seat", "20", 4);
    events.publish(event.getId());
  }

  private Order paid(int quantity) {
    Order order = checkout.start(event.getId(), Map.of(tier.getId(), quantity));
    return checkout.complete(order.getAccessToken(), new CheckoutForm("Ana", "ana@example.com"));
  }

  @Test
  void cancellingReleasesTheSeats() {
    Order order = paid(4);
    assertThat(availability.forEvent(event.getId()).get(tier.getId()).isSoldOut()).isTrue();

    Order cancelled = admin.cancel(order.getId());

    assertThat(cancelled.getStatus()).isEqualTo(OrderStatus.CANCELLED);
    assertThat(cancelled.getCancelledAt()).isEqualTo(clock.instant());
    assertThat(availability.forEvent(event.getId()).get(tier.getId()).remaining()).isEqualTo(4);
  }

  @Test
  void cannotCancelTwice() {
    Order order = paid(1);
    admin.cancel(order.getId());

    assertThatThrownBy(() -> admin.cancel(order.getId())).hasMessageContaining("already");
  }

  @Test
  void adminCanCancelFromTheOrderPage() throws Exception {
    Order order = paid(1);

    mvc.perform(post("/admin/orders/" + order.getId() + "/cancel").with(csrf()).with(user("a")))
        .andExpect(flash().attributeExists("flash"));

    assertThat(admin.get(order.getId()).getStatus()).isEqualTo(OrderStatus.CANCELLED);
  }
}

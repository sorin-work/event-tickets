package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.sorinwork.tickets.event.Event;
import com.sorinwork.tickets.promo.DiscountType;
import com.sorinwork.tickets.promo.PromoCode;
import com.sorinwork.tickets.promo.PromoCodeRepository;
import com.sorinwork.tickets.support.IntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.test.context.support.WithMockUser;

@WithMockUser(roles = "ADMIN")
class AdminPromoControllerTest extends IntegrationTest {

  @Autowired PromoCodeRepository promos;

  Event event;
  String base;

  @BeforeEach
  void setUp() {
    event = data.event("Comedy Night");
    base = "/admin/events/" + event.getId() + "/promo-codes";
  }

  @Test
  void createsPercentageCode() throws Exception {
    mvc.perform(
            post(base)
                .with(csrf())
                .param("code", " friends20 ")
                .param("discountType", "PERCENT")
                .param("amount", "20")
                .param("maxUses", "50"))
        .andExpect(status().is3xxRedirection());

    PromoCode promo = promos.findByEventIdAndCode(event.getId(), "FRIENDS20").orElseThrow();
    assertThat(promo.getAmount()).isEqualTo(20);
    assertThat(promo.getMaxUses()).isEqualTo(50);

    mvc.perform(get(base)).andExpect(content().string(containsString("FRIENDS20")));
  }

  @Test
  void createsFixedCodeInCents() throws Exception {
    mvc.perform(
        post(base)
            .with(csrf())
            .param("code", "FIVEOFF")
            .param("discountType", "FIXED")
            .param("amount", "5.00"));

    PromoCode promo = promos.findByEventIdAndCode(event.getId(), "FIVEOFF").orElseThrow();
    assertThat(promo.getDiscountType()).isEqualTo(DiscountType.FIXED);
    assertThat(promo.getAmount()).isEqualTo(500);
  }

  @Test
  void rejectsPercentOverHundred() throws Exception {
    mvc.perform(
            post(base)
                .with(csrf())
                .param("code", "TOOMUCH")
                .param("discountType", "PERCENT")
                .param("amount", "120"))
        .andExpect(model().attributeHasFieldErrors("form", "amountValid"));
  }

  @Test
  void rejectsDuplicateCodes() throws Exception {
    for (int i = 0; i < 2; i++) {
      mvc.perform(
          post(base)
              .with(csrf())
              .param("code", "VIP")
              .param("discountType", "PERCENT")
              .param("amount", "10"));
    }

    assertThat(promos.findByEventIdOrderByCode(event.getId())).hasSize(1);
  }

  @Test
  void disablesCode() throws Exception {
    mvc.perform(
        post(base)
            .with(csrf())
            .param("code", "VIP")
            .param("discountType", "PERCENT")
            .param("amount", "10"));
    PromoCode promo = promos.findByEventIdAndCode(event.getId(), "VIP").orElseThrow();

    mvc.perform(post(base + "/" + promo.getId() + "/active").with(csrf()).param("active", "false"))
        .andExpect(status().is3xxRedirection());

    assertThat(promos.findById(promo.getId()).orElseThrow().isActive()).isFalse();
  }
}

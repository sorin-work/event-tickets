package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;

import com.sorinwork.tickets.common.BusinessException;
import com.sorinwork.tickets.support.IntegrationTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminAccountsTest extends IntegrationTest {

  @Autowired AdminAccountService accounts;
  @Autowired AdminUserRepository users;
  @Autowired PasswordEncoder passwordEncoder;

  AdminUser owner;

  @BeforeEach
  void setUp() {
    owner = accounts.create("owner@example.com", "Owner", "first-password");
  }

  @Test
  void createsAdmins() throws Exception {
    mvc.perform(
            post("/admin/users")
                .with(csrf())
                .with(user("owner@example.com"))
                .param("email", "Door@Example.com")
                .param("name", "Door staff")
                .param("password", "door-password"))
        .andExpect(flash().attribute("flash", "door@example.com can now sign in."));

    mvc.perform(get("/admin/users").with(user("owner@example.com")))
        .andExpect(content().string(Matchers.containsString("Door staff")));
  }

  @Test
  void rejectsShortPasswordsAndDuplicates() {
    assertThatThrownBy(() -> accounts.create("a@example.com", "A", "short"))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("at least 10");
    assertThatThrownBy(() -> accounts.create("OWNER@example.com", "Again", "long-enough-pw"))
        .hasMessageContaining("already");
  }

  @Test
  void cannotRemoveYourself() {
    AdminUser other = accounts.create("other@example.com", "Other", "other-password");

    assertThatThrownBy(() -> accounts.delete(owner.getId(), "owner@example.com"))
        .hasMessageContaining("own account");
    accounts.delete(other.getId(), "owner@example.com");
    assertThat(users.count()).isEqualTo(1);
  }

  @Test
  void changesPassword() throws Exception {
    mvc.perform(
            post("/admin/account/password")
                .with(csrf())
                .with(user("owner@example.com"))
                .param("currentPassword", "first-password")
                .param("newPassword", "second-password")
                .param("confirmation", "second-password"))
        .andExpect(flash().attribute("flash", "Password changed."));

    AdminUser reloaded = users.findByEmail("owner@example.com").orElseThrow();
    assertThat(passwordEncoder.matches("second-password", reloaded.getPasswordHash())).isTrue();
  }

  @Test
  void passwordChangeNeedsTheCurrentPasswordAndAMatchingRepeat() {
    assertThatThrownBy(
            () ->
                accounts.changePassword(
                    "owner@example.com", "wrong", "second-password", "second-password"))
        .hasMessageContaining("current password");
    assertThatThrownBy(
            () ->
                accounts.changePassword(
                    "owner@example.com", "first-password", "second-password", "typo-password"))
        .hasMessageContaining("do not match");
  }
}

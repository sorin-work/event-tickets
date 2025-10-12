package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;

import com.sorinwork.tickets.config.AppProperties;
import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Clock;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;

class AdminBootstrapTest extends IntegrationTest {

  @Autowired AdminUserRepository users;
  @Autowired PasswordEncoder passwordEncoder;
  @Autowired Clock clock;

  private AdminBootstrap bootstrap(String email, String password) {
    return new AdminBootstrap(
        users,
        passwordEncoder,
        new AppProperties(new AppProperties.Admin(email, password, "Owner")),
        clock);
  }

  @Test
  void createsAdminWhenNoneExists() {
    assertThat(bootstrap("owner@example.com", "s3cret-pass").createInitialAdmin()).isTrue();

    AdminUser admin = users.findByEmail("owner@example.com").orElseThrow();
    assertThat(admin.getDisplayName()).isEqualTo("Owner");
    assertThat(passwordEncoder.matches("s3cret-pass", admin.getPasswordHash())).isTrue();
  }

  @Test
  void doesNothingWhenAnAdminAlreadyExists() {
    users.save(new AdminUser("first@example.com", "First", "hash", Instant.now()));

    assertThat(bootstrap("owner@example.com", "s3cret-pass").createInitialAdmin()).isFalse();
    assertThat(users.count()).isEqualTo(1);
  }

  @Test
  void doesNothingWithoutCredentials() {
    assertThat(bootstrap("", "").createInitialAdmin()).isFalse();
    assertThat(users.count()).isZero();
  }
}

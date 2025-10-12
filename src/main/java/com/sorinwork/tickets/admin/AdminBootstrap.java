package com.sorinwork.tickets.admin;

import com.sorinwork.tickets.config.AppProperties;
import java.time.Clock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/** Creates the first admin from configuration so a fresh install can be signed into. */
@Component
public class AdminBootstrap implements ApplicationRunner {

  private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

  private final AdminUserRepository users;
  private final PasswordEncoder passwordEncoder;
  private final AppProperties properties;
  private final Clock clock;

  public AdminBootstrap(
      AdminUserRepository users,
      PasswordEncoder passwordEncoder,
      AppProperties properties,
      Clock clock) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public void run(ApplicationArguments args) {
    createInitialAdmin();
  }

  public boolean createInitialAdmin() {
    AppProperties.Admin admin = properties.admin();
    if (!StringUtils.hasText(admin.email()) || !StringUtils.hasText(admin.password())) {
      return false;
    }
    if (users.count() > 0) {
      return false;
    }
    users.save(
        new AdminUser(
            admin.email(),
            admin.name(),
            passwordEncoder.encode(admin.password()),
            clock.instant()));
    log.info("Created initial admin account {}", AdminUser.normalizeEmail(admin.email()));
    return true;
  }
}

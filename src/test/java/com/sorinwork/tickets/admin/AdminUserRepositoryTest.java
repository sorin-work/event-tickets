package com.sorinwork.tickets.admin;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sorinwork.tickets.support.IntegrationTest;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;

class AdminUserRepositoryTest extends IntegrationTest {

  @Autowired AdminUserRepository repository;

  @Test
  void storesEmailsInLowerCase() {
    repository.save(new AdminUser("  Ana@Example.COM ", "Ana", "hash", Instant.now()));

    assertThat(repository.findByEmail("ana@example.com")).isPresent();
    assertThat(repository.existsByEmail("ana@example.com")).isTrue();
  }

  @Test
  void rejectsDuplicateEmails() {
    repository.saveAndFlush(new AdminUser("ana@example.com", "Ana", "hash", Instant.now()));

    assertThatThrownBy(
            () ->
                repository.saveAndFlush(
                    new AdminUser("ANA@example.com", "Ana 2", "hash", Instant.now())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }
}

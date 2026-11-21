package com.sorinwork.tickets.support;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Base class for tests that need a real PostgreSQL, mostly races between transactions where H2's
 * locking is only an imitation. They run when {@code TEST_DATABASE_URL} points at an empty database
 * (CI starts one) and are skipped otherwise. Flyway migrates it like production.
 */
@SpringBootTest
@ActiveProfiles("test")
@EnabledIfEnvironmentVariable(named = "TEST_DATABASE_URL", matches = ".+")
@Import({DatabaseCleaner.class, TestData.class, Purchases.class, TestClockConfig.class})
public abstract class PostgresTest {

  @Autowired protected TestData data;
  @Autowired protected TestClock clock;
  @Autowired protected Purchases purchases;
  @Autowired private DatabaseCleaner databaseCleaner;

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registry) {
    registry.add("spring.datasource.url", () -> System.getenv("TEST_DATABASE_URL"));
    registry.add("spring.datasource.username", () -> env("TEST_DATABASE_USER", "tickets"));
    registry.add("spring.datasource.password", () -> env("TEST_DATABASE_PASSWORD", "tickets"));
  }

  private static String env(String name, String fallback) {
    String value = System.getenv(name);
    return value == null || value.isBlank() ? fallback : value;
  }

  @BeforeEach
  void cleanDatabase() {
    databaseCleaner.clean();
    clock.reset();
  }
}

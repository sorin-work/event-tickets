package com.sorinwork.tickets.support;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/** Base class for tests that boot the whole application against an in-memory H2 database. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import({DatabaseCleaner.class, TestData.class, Purchases.class, TestClockConfig.class})
public abstract class IntegrationTest {

  @Autowired protected MockMvc mvc;
  @Autowired protected TestData data;
  @Autowired protected TestClock clock;
  @Autowired protected Purchases purchases;
  @Autowired private DatabaseCleaner databaseCleaner;
  @Autowired private com.sorinwork.tickets.config.CheckoutLimits checkoutLimits;

  @BeforeEach
  void cleanDatabase() {
    databaseCleaner.clean();
    clock.reset();
    checkoutLimits.clear();
  }
}

package com.sorinwork.tickets.support;

import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/** Empties every application table between integration tests. */
@Component
public class DatabaseCleaner {

  private final JdbcTemplate jdbc;

  public DatabaseCleaner(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  public void clean() {
    List<String> tables =
        jdbc.queryForList(
            "select table_name from information_schema.tables "
                + "where table_schema = 'public' and table_type = 'BASE TABLE' "
                + "and table_name <> 'flyway_schema_history'",
            String.class);
    if (isPostgres()) {
      jdbc.execute("TRUNCATE TABLE " + String.join(", ", tables) + " RESTART IDENTITY CASCADE");
      return;
    }
    jdbc.execute("SET REFERENTIAL_INTEGRITY FALSE");
    try {
      for (String table : tables) {
        jdbc.execute("TRUNCATE TABLE " + table + " RESTART IDENTITY");
      }
    } finally {
      jdbc.execute("SET REFERENTIAL_INTEGRITY TRUE");
    }
  }

  private boolean isPostgres() {
    String product =
        jdbc.execute(
            (java.sql.Connection connection) -> connection.getMetaData().getDatabaseProductName());
    return "PostgreSQL".equals(product);
  }
}

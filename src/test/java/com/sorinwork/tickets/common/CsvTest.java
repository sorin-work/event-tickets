package com.sorinwork.tickets.common;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.StringWriter;
import org.junit.jupiter.api.Test;

class CsvTest {

  @Test
  void writesRowsWithCrlf() {
    StringWriter out = new StringWriter();
    new Csv(out).row("name", "email").row("Ana", null);

    assertThat(out.toString()).isEqualTo("name,email\r\nAna,\r\n");
  }

  @Test
  void quotesSpecialCharacters() {
    assertThat(Csv.escape("Pop, Ana")).isEqualTo("\"Pop, Ana\"");
    assertThat(Csv.escape("say \"hi\"")).isEqualTo("\"say \"\"hi\"\"\"");
    assertThat(Csv.escape("two\nlines")).isEqualTo("\"two\nlines\"");
  }

  @Test
  void defusesFormulas() {
    assertThat(Csv.escape("=HYPERLINK(\"x\")")).startsWith("\"'=");
    assertThat(Csv.escape("+40 712 345")).isEqualTo("'+40 712 345");
  }
}

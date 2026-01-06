package com.sorinwork.tickets.common;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.io.Writer;

/** Minimal RFC 4180 writer: fields are quoted when they contain commas, quotes or newlines. */
public final class Csv {

  private final Writer out;

  public Csv(Writer out) {
    this.out = out;
  }

  public Csv row(Object... fields) {
    try {
      for (int i = 0; i < fields.length; i++) {
        if (i > 0) {
          out.write(',');
        }
        out.write(escape(fields[i] == null ? "" : fields[i].toString()));
      }
      out.write("\r\n");
      return this;
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }

  static String escape(String value) {
    String safe = value;
    // Keep spreadsheet apps from treating cells as formulas.
    if (!safe.isEmpty() && "=+-@".indexOf(safe.charAt(0)) >= 0) {
      safe = "'" + safe;
    }
    if (safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r")) {
      return '"' + safe.replace("\"", "\"\"") + '"';
    }
    return safe;
  }
}

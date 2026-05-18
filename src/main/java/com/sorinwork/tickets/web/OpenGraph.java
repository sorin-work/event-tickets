package com.sorinwork.tickets.web;

import com.sorinwork.tickets.event.Event;

/** Link preview data for chat apps and social sites. */
public record OpenGraph(String title, String description, String url) {

  static final int MAX_DESCRIPTION = 200;

  public static OpenGraph of(Event event, String when, String url) {
    String summary = when + " · " + event.getVenueName();
    if (event.getDescription() != null) {
      summary += ". " + event.getDescription().replaceAll("\\s+", " ").trim();
    }
    if (summary.length() > MAX_DESCRIPTION) {
      summary = summary.substring(0, MAX_DESCRIPTION - 1).trim() + "…";
    }
    return new OpenGraph(event.getTitle(), summary, url);
  }
}

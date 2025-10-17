package com.sorinwork.tickets.event;

public enum EventStatus {
  /** Being set up; only visible to admins. */
  DRAFT,
  /** Visible on the public site and open for sales. */
  PUBLISHED,
  /** Called off; the page stays up with a notice but nothing can be bought. */
  CANCELLED
}

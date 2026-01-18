package com.sorinwork.tickets.waitlist;

public enum WaitlistStatus {
  /** In the queue for released tickets. */
  WAITING,
  /** Tickets are held for this person; they have been sent a checkout link. */
  OFFERED,
  /** They bought the offered tickets. */
  CLAIMED,
  /** The offer ran out without a purchase. */
  EXPIRED,
  /** Taken off the list by an admin. */
  REMOVED
}

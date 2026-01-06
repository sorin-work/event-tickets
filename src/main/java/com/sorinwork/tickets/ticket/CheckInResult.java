package com.sorinwork.tickets.ticket;

/** Outcome of scanning a ticket at the door. */
public record CheckInResult(Status status, Ticket ticket) {

  public enum Status {
    /** Valid ticket, now checked in. Let them in. */
    CHECKED_IN,
    /** Valid ticket that was already used. */
    ALREADY_CHECKED_IN,
    /** No ticket with that code. */
    NOT_FOUND,
    /** The ticket is for a different event. */
    WRONG_EVENT,
    /** The ticket's order was cancelled. */
    CANCELLED
  }

  public boolean admitted() {
    return status == Status.CHECKED_IN;
  }

  public static CheckInResult of(Status status, Ticket ticket) {
    return new CheckInResult(status, ticket);
  }
}

package com.sorinwork.tickets.event;

/** Published when an admin cancels an event, so its orders can be cancelled too. */
public record EventCancelled(long eventId) {}

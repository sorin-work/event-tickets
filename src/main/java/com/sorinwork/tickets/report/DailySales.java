package com.sorinwork.tickets.report;

import java.time.LocalDate;

/** Tickets sold and money taken on one day, in the event's time zone. */
public record DailySales(LocalDate date, long tickets, long netCents) {}

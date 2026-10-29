# Changelog

## 1.0.0 - 2026-10-29

First stable release.

### Events and tickets

- Events with ticket tiers, prices, quantities, per-order limits, sales windows
  in the venue's time zone, an optional venue capacity and paused tiers
- Publish, unpublish, cancel, duplicate and delete draft events; change an
  event's address
- Public event pages with remaining tickets, calendar (.ics) files, link
  previews and a map link; search and a page of past events

### Checkout

- Seats are held for 10 minutes while the buyer checks out, with a countdown
- Simulated payment, one ticket per seat with an optional name per ticket
- Promo codes: percentage or fixed, usage limits, expiry and tier-only codes
- Ticket emails with QR code links, "find my tickets" and a reminder the day
  before the event

### Waitlist

- Join when a tier is sold out, see your place, leave from the email link
- Released tickets (expired checkouts, cancelled orders) are offered in the
  order people joined and held for 24 hours

### At the door and in the admin area

- Check-in page with camera scanning, beeps, a live counter and latest arrivals;
  manual check-in and undo
- Attendee list with search, arrival filter, CSV export and print view
- Orders with filters, notes, cancellation, resending tickets and CSV export
- Sales report with tiers, revenue, promo codes, sales by day and arrivals per
  hour, also as CSV
- Admin accounts, password changes and an outbox of every email sent
- JSON API for event listings and door scanner apps (HTTP Basic)

### Operations

- Docker Compose setup with Postgres, healthchecks and demo data
- Flyway migrations, request ids in logs, security headers, reverse proxy
  support

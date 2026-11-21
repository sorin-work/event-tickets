# Architecture notes

One Spring Boot application renders every page with Thymeleaf, uses htmx for
the few places that update without a full reload, and keeps its data in
PostgreSQL. No front-end build, no message broker, no separate worker.

## Packages

| Package    | What lives there                                                            |
| ---------- | --------------------------------------------------------------------------- |
| `event`    | Events, ticket tiers, slugs, copying events, calendar files                 |
| `order`    | Checkout holds, orders, availability, hold expiry, admin order actions      |
| `promo`    | Promo codes and the discount calculation                                    |
| `ticket`   | Issued tickets, QR codes, check-in, the attendee list                       |
| `waitlist` | Waitlist entries and offering released tickets                              |
| `notify`   | The outbox and the plain-text emails sent to buyers                         |
| `report`   | Sales figures, sales by day and arrivals per hour                           |
| `admin`    | Admin controllers, admin accounts and sign-in                               |
| `web`      | Public controllers and template helpers (`@fmt`)                            |
| `api`      | The JSON API                                                                |
| `config`   | Security, scheduling, the clock, `app.*` properties, request ids            |
| `seed`     | Demo data for `SEED_DEMO=true`                                              |

## Holding seats

Availability is never stored. For each tier it is computed from order items:

```
remaining = quantity - sold (PAID orders) - held (PENDING orders whose hold has not run out)
```

capped by what is left of the event's venue capacity, if it has one.

Starting a checkout locks the event row (`SELECT ... FOR UPDATE`), recomputes
availability and saves a `PENDING` order with `hold_expires_at = now + 10m`.
Two buyers racing for the last seat are serialised by that lock. A hold that
runs out stops counting immediately; `HoldExpiryService` marks such orders
`EXPIRED` every 30 seconds so admin pages and reports stay accurate.

Paying (simulated) moves the order to `PAID` and issues one ticket per seat.
Orders carry a `version` column so a double submit cannot pay twice.

## Checking in

A scan is one conditional `UPDATE` on the ticket: it sets `checked_in_at` only
where it is still empty and the ticket's order is still `PAID`. Two phones
scanning the same code at the same moment both run it, the database makes the
second wait for the first, and the second then changes no row and answers
"already checked in at 19:42 by north door". The same applies to the attendee
list's manual check-in and the door API, which call the same service. Undoing
a check-in is the reverse update. Before the update the scan takes a shared
lock on the order (`FOR SHARE`), so a cancellation committing at that moment
either waits for the scan or is seen by it.

## Waitlist

Whenever tickets go back on sale (a hold expires, an order or event is
cancelled) an `OrderEvents.TicketsReleased` event is published. The waitlist
service then walks the waiting entries of each tier in the order people joined.
For each person whose request fits in what is free it creates a `PENDING`
order on their behalf, held for 24 hours, and emails them its checkout link.
Someone asking for more tickets than are free keeps their place and blocks the
people behind them, so nobody is skipped.

## Promo codes

A code's uses are not stored. They are counted from orders when needed: paid
orders plus checkouts that still hold their seats. A checkout that runs out or
an order that is cancelled therefore gives its use back automatically. Codes
can be limited to one tier; then only that tier's tickets are discounted.
Percentages are rounded half up to the cent.

Applying a code locks its row (`SELECT ... FOR UPDATE`) in the query that
finds it, before counting its uses. Applying is the only thing that adds a use,
so two buyers cannot both take the last one. Changing a code's limits or
switching it off takes the same lock, and codes carry a `version` column like
orders. Payment looks at the code once more: if it was switched
off in the meantime, the discount is removed, nothing is charged and the
checkout page shows the new total. A code whose end date passes while the
buyer's seats are held is still honoured, because it was valid when applied.

## Scheduled jobs

Both run inside the app (`app.scheduling.enabled`, on by default):

| Job                   | Every   | What it does                                                   |
| --------------------- | ------- | -------------------------------------------------------------- |
| `HoldExpiryService`   | 30 s    | Expires unpaid checkouts and hands their seats to the waitlist |
| `ReminderService`     | 15 min  | Emails buyers about 24 hours before their event starts         |

With several app instances each would run the jobs; the work is idempotent
(status checks, `reminder_sent_at`), but one instance is the expected setup.

## Time

Event start/end times and tier sales windows are wall-clock `LocalDateTime`s in
the event's own time zone, because that is how organisers think about them.
Everything that happens (orders, holds, check-ins) is an `Instant` in UTC.
Conversions go through `Event.zone()`. The application `Clock` is a bean so
tests can move time forward.

## Emails

There is no SMTP integration. `Notifier` writes every email to the
`outbox_messages` table and logs it; admins can read them under **Outbox**.
Sending them for real would mean adding a sender that drains the outbox.

## Security

- `/admin/**` needs a signed-in admin (form login, sessions, CSRF tokens; htmx
  sends the token from a meta tag).
- `/api/v1/admin/**` has its own stateless filter chain with HTTP Basic.
- Buyers never log in: their links contain an unguessable access token, and
  QR codes are only served under that link.
- A content security policy only allows scripts and styles from the app.

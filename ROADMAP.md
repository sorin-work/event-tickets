# Roadmap

Things I might add to event-tickets. The app covers a small venue's whole
evening today (publish, sell, hold, waitlist, scan at the door, report), so
nothing here is promised or scheduled. These are ideas, written down with
enough detail that I (or anyone else) can pick one up later without starting
from zero. Items may change, move between groups or be dropped.

What the app does today is in the [README](README.md) and the notes under
[docs/](docs/).

- **Next**: small or well understood. I would start with these.
- **Later**: worth doing, but bigger or waiting on a decision.
- **Maybe**: not sure yet that they are worth the extra complexity.

Sizes are rough: **S** is an evening or two, **M** a few evenings up to a
couple of weekends, **L** several weeks of spare time.

Each item is meant to fit in one pull request and to be covered by the test
suite. There are no browser tests. Unit tests cover pure code (records such
as `TierAvailability`, `DiscountCalculator`, `CalendarFile`, the helpers in
`common`). Integration tests extend `support/IntegrationTest`: they boot the
whole application against H2 in PostgreSQL mode, drive pages through MockMvc
and move the shared `TestClock` forward to let holds and offers run out. So
rules worth testing belong in a service of the feature's package (`order`,
`waitlist`, `ticket` ...), not in a controller or a template. Controllers stay
thin and turn a `BusinessException` into a message on the page. Items that add
columns bring their own Flyway migration under `src/main/resources/db/migration`
with the next free `V<n>__` number. File names below are relative to
`src/main/java/com/sorinwork/tickets/` unless they start with `src/`, `docs/`
or a file at the top of the repository.

Selling tickets means money and a fixed number of seats. Three things are the
hardest part of most items below and get the most care: never selling more
seats than exist, never taking money twice for one order, and keeping seat
holds honest (a hold either ends in a paid order or gives its seats back,
to the waitlist first). Anything that changes how many seats are free runs
under the event row lock (`EventRepository.lockById`) and publishes
`OrderEvents.TicketsReleased` when seats come back.

"Depends on" lists real prerequisites only. Items that merely touch the same
file are not listed there; the bigger overlaps are mentioned in the items.

When an item ships, its group cell in the Overview becomes `Shipped`, its
section moves under a `## Shipped` heading at the end of this file and shrinks
to one paragraph (what was built, how it differs from the plan, what is still
open), and the README, the notes under `docs/` and the `Unreleased` heading at
the top of the CHANGELOG are updated in the same pull request.

## Overview

| #  | Item                                         | Group   | Area     | Size | Depends on |
| -- | -------------------------------------------- | ------- | -------- | ---- | ---------- |
| 1  | Check-in that cannot admit a ticket twice    | Shipped | door     | S    | -          |
| 2  | Promo code limits that hold under load       | Shipped | checkout | S    | -          |
| 3  | Letting go of a hold                         | Next    | checkout | S    | -          |
| 4  | Changing quantities without overselling      | Next    | events   | M    | -          |
| 5  | A cap on the seats one visitor can hold      | Next    | checkout | M    | 3          |
| 6  | Race tests against real PostgreSQL           | Later   | tests    | M    | -          |
| 7  | Waitlist offers that follow the tier's rules | Later   | waitlist | S    | -          |
| 8  | Refunds recorded with cancellations          | Later   | orders   | M    | -          |
| 9  | Cancelling single tickets                    | Later   | orders   | M    | 8          |
| 10 | Real payments through a provider             | Later   | checkout | L    | 8          |
| 11 | Sending email for real                       | Later   | notify   | M    | -          |
| 12 | Throttled admin sign-in                      | Later   | security | S    | -          |
| 13 | Limits on public forms that send email       | Later   | security | S    | -          |
| 14 | Door staff accounts                          | Later   | admin    | M    | -          |
| 15 | Scanner tokens for the door API              | Later   | api      | M    | 14         |
| 16 | Audit log of admin actions                   | Later   | admin    | M    | -          |
| 17 | Scheduled backups and a tested restore       | Later   | ops      | S    | -          |
| 18 | Upgrade guide and a migration check          | Later   | ops      | S    | 6          |
| 19 | Scheduled jobs with several instances        | Later   | ops      | M    | -          |
| 20 | Metrics for operators                        | Later   | ops      | S    | -          |
| 21 | Accessibility pass                           | Later   | ui       | M    | -          |
| 22 | Complimentary tickets and a guest list       | Later   | orders   | S    | -          |
| 23 | Selling at the door                          | Later   | door     | M    | 22         |
| 24 | Buyers passing a ticket on                   | Later   | tickets  | M    | -          |
| 25 | Questions at checkout                        | Later   | checkout | M    | -          |
| 26 | Hidden tiers behind an access code           | Later   | events   | M    | -          |
| 27 | Tiers that take several seats                | Later   | events   | M    | -          |
| 28 | Booking fees and tax on orders               | Later   | orders   | M    | -          |
| 29 | Buyers cancelling before a deadline          | Later   | orders   | M    | 8          |
| 30 | Event series                                 | Later   | events   | M    | -          |
| 31 | Forgetting buyers after an event             | Later   | privacy  | M    | -          |
| 32 | Sales across all events                      | Later   | report   | S    | -          |
| 33 | Pay what you want                            | Maybe   | events   | M    | -          |
| 34 | Webhooks for orders and check-ins            | Maybe   | api      | M    | -          |
| 35 | An embeddable ticket widget                  | Maybe   | web      | M    | -          |
| 36 | Translations                                 | Maybe   | ui       | M    | -          |
| 37 | Reserved seating with a seat map             | Maybe   | events   | L    | -          |
| 38 | Offline check-in for scanner apps            | Maybe   | door     | L    | 1, 15      |
| 39 | Wallet passes                                | Maybe   | tickets  | L    | -          |

## Next

### 3. Letting go of a hold

A hold can only run out. A buyer who picked three tickets instead of two goes
back to the event page and starts again: the first checkout keeps its seats
for ten minutes and the second takes more. With five tickets left, one
undecided buyer can make the event look sold out for everyone else, and the
waitlist waits for nothing. The checkout page has no way to give the seats
back.

- `CheckoutService.release(token)`: lock the event, and if the order is still
  `PENDING` mark it `EXPIRED` (the same status a hold that ran out gets),
  publish `HoldExpired` and `TicketsReleased`. Paid, cancelled or already
  expired orders are left alone and the call is a no-op, so a release racing
  a payment is decided by the order's `version` like a double submit.
- A "Cancel and give the tickets back" button on the checkout page
  (`POST /checkout/{token}/release`), back to the event page with a message.
- Starting again from the same browser: `start` sets a cookie per event
  (`hold_<eventId>`, the access token, `HttpOnly`, `SameSite=Lax`, lifetime of
  the hold). When the same browser starts another checkout for that event, the
  previous hold is released in the same transaction before availability is
  computed, so the buyer can take their own seats again.
- Tricky: a waitlist offer is also a `PENDING` order. Releasing it is the
  person declining the offer. `WaitlistService.onHoldExpired` already marks
  the entry `EXPIRED`, and `TicketsReleased` offers the seats to the next
  person. The offer page gets a "No thanks" wording instead of "Cancel".
- Tricky: the cookie is only a hint. The token in it is checked like any
  other (same event, still pending); a stale or forged cookie releases nothing
  that is not the browser's own.
- Left out: releasing on tab close (`sendBeacon`). It is not reliable, and the
  buyer may still have the checkout open in another tab.
- Tests: integration tests in `order/CheckoutServiceTest` and
  `web/CheckoutControllerTest`: release frees seats at once, a second release
  is harmless, release after payment does nothing, starting again from the
  same browser reuses the seats, a released waitlist offer goes to the next
  person.

### 4. Changing quantities without overselling

`TierService.update` writes the new `quantity` straight to the tier and
`EventService.update` does the same with the venue `capacity`. Neither takes
the event lock and neither looks at what is sold. Two problems follow:

- Lowering below what is sold plus held is accepted. Availability clamps to
  zero so nothing new is sold, but every open checkout can still be paid, and
  then more seats are sold than the tier or venue has. The admin is not told.
- Raising the quantity or capacity, or un-pausing a tier, puts seats on sale
  without publishing `TicketsReleased`, so the people on the waitlist are
  skipped and the new seats go to whoever refreshes the page first.

The plan:

- Both updates lock the event (`lockById`) and compute availability before
  writing. A tier quantity below `sold + held`, or a capacity below the total
  taken across tiers, is refused with "30 are sold and 2 are held in open
  checkouts; the holds end at 19:42". Refusing below held seats too is the
  only way the number can never be exceeded.
- After a change that frees seats (quantity or capacity up, tier un-paused, a
  sales start moved into the past) the service flushes and publishes
  `TicketsReleased`, so the waitlist gets them first.
- Tier delete (`TierService.delete`) already refuses tiers with orders; no
  change.
- Tricky: the check must use the same numbers as checkout. Reuse
  `AvailabilityService.forEvent` inside the lock rather than writing a second
  count query.
- Tricky: the event form also changes the time zone and start time. Only the
  capacity part needs the lock; the rest saves as today.
- Undecided: let an admin knowingly oversell (a guest list on top of a sold
  out venue). No: comps are item 22 and count against the capacity.
- Tests: `admin/AdminTierControllerTest` and `order/EventCapacityTest`: refused
  below sold, refused below sold plus held, accepted at exactly that number,
  raising a sold-out tier with people waiting makes an offer, un-pausing makes
  an offer.

### 5. A cap on the seats one visitor can hold

Holding seats costs nothing. A script that fetches the event page (for the
CSRF token) and posts `/events/{slug}/checkout` fifty times holds fifty times
`max_per_order` seats for ten minutes, and can do it again when they run out.
A popular event looks sold out all evening and the waitlist fills with real
buyers. There is no limit on checkout starts anywhere.

- A client key on each order: a hash of the client address (with
  `forward-headers-strategy: native` this is the `X-Forwarded-For` client
  behind a proxy) and an app secret, in a new `client_key` column. Not the raw
  address, so the table does not become a log of buyers' IPs.
- In `CheckoutService.start`, under the event lock that is already taken,
  count the seats held by open checkouts with the same key for this event. If
  the new request would take it over `app.checkout.max-held-seats` (default
  12), refuse with "You already have tickets reserved; finish or cancel that
  checkout first" and a link to it when the browser cookie from item 3 knows
  it.
- A small in-memory rate limit on checkout starts per key (say 20 a minute),
  in a `RateLimiter` under `config/`. It is per process, which is fine for the
  single instance the architecture notes expect.
- Waitlist holds have no client key and never count; they are made by the
  app.
- Tricky: shared addresses. A school or a venue's wifi puts many honest buyers
  behind one IP. The default is generous, it is per event, and item 3 makes
  an honest retry release its own hold first, which is why this depends on 3.
  `0` turns the cap off.
- Tricky: behind a proxy that does not send `X-Forwarded-For`, every buyer
  has the proxy's address and shares one cap. The README section on reverse
  proxies says so, next to the existing header lines.
- Left out: a captcha. It needs a third-party script, which the content
  security policy does not allow, and a cap catches the cheap attack.
- Tests: integration tests: a second checkout from the same address over the
  cap is refused, another address is not affected, expired holds stop
  counting, cap `0` means no limit, waitlist holds are ignored.

## Later

### 6. Race tests against real PostgreSQL

Every test runs on H2. The guarantees that matter most (two buyers and the
last seat, two scanners and one ticket, two appliers and one promo use) rest
on `SELECT ... FOR UPDATE` and on how PostgreSQL waits for row locks, and H2
only imitates that. No test today starts two threads.

- Testcontainers with `postgres:17-alpine` (the compose image) for a small
  separate set of tests, tagged so they run in `./mvnw verify` when Docker is
  there. CI's runner has Docker; the job already builds an image.
- A helper that runs N callables at once behind a `CountDownLatch` and
  collects the results, each in its own transaction.
- Cases: 20 buyers for 5 seats sell exactly 5; capacity shared by two tiers
  is never exceeded; 10 scanners and one ticket admit once (item 1); 10
  appliers of a one-use code (item 2); a payment and a hold expiry at the
  same time leave one outcome.
- Tricky: `IntegrationTest` shares one Spring context on H2. These tests need
  their own context and their own cleaner (`TRUNCATE ... CASCADE`, no
  `SET REFERENTIAL_INTEGRITY`).
- Undecided: move every integration test to PostgreSQL. Slower, and the H2
  suite has been fine for everything that is not a race. I would keep both.

### 7. Waitlist offers that follow the tier's rules

`WaitlistService.offerReleasedTickets` offers seats whenever some are free.
It does not look at the tier: a paused tier, a tier whose sales ended, and an
event that has already started all still get offers, and the email asks
people to pay for something the public page says is not on sale. Separately,
when an admin cancels an offer's `PENDING` order, `cancel` publishes only
`TicketsReleased`, so the entry stays `OFFERED` forever and the person cannot
join again.

- Skip tiers that are paused or outside their sales window, and events that
  have started, using the same rule as `CheckoutService.checkOnSale` (moved to
  one place so the two cannot drift).
- When a tier is un-paused or its window opens, item 4 publishes
  `TicketsReleased` and the offers start.
- Cancelling an order that is an offer marks the entry `EXPIRED` (a listener
  on a new `OrderCancelled` event, next to `HoldExpired`).
- Tests: `waitlist/WaitlistOfferTest`: no offer on a paused tier, none after
  sales end, none after the start, cancelled offer frees the entry.

### 8. Refunds recorded with cancellations

Cancelling a paid order says "after a refund outside the app". The app does
not record whether money went back, how much, or when. The sales report then
counts nothing for a cancelled order, which is right for sales but leaves no
trace of what was refunded.

- A `refunds` table: order, amount in cents, reason, admin, time. The cancel
  form asks for the refunded amount (default the order total, can be less, for
  example keeping a fee) and refuses more than was paid minus earlier refunds.
- The order page shows the refunds; the report gets a "refunded" line and the
  CSV a column.
- Tricky: a refund is money, so amounts never come from a recomputation of
  the order. They are stored as entered and summed.
- Tests: refund at cancel, partial refund, refusing more than paid, report
  totals.

### 9. Cancelling single tickets

One of four friends cannot come. Today the admin can only cancel the whole
order. A per-ticket cancel would void that ticket's code at the door, put its
seat back on sale (and to the waitlist) and record a refund for its share.

- `cancelled_at` on `Ticket`. Availability today counts order items of paid
  orders; it must subtract cancelled tickets, in the same queries
  (`OrderItemRepository.countsForEvent` and `countsForEvents`).
- The refund for one ticket is its unit price minus its share of the order
  discount, rounded so that cancelling every ticket one by one refunds exactly
  the order total. That rounding is the part to test hardest.
- Tricky: the check-in update from item 1 must also require the ticket itself
  not cancelled.
- Depends on 8 for the refund record.

### 10. Real payments through a provider

Payment is simulated: `complete` marks the order paid. Taking real money
means a hosted payment page (Stripe Checkout is the usual choice for a small
organiser) and its webhooks, and this is where double charging and
overselling meet.

- A `PaymentProvider` interface with the simulated one kept for the demo and
  tests, and one real implementation selected by configuration.
- The checkout form creates a payment session with the order's reference as
  idempotency key and redirects to it. The order becomes `PAID` only from the
  provider's signed webhook, never from the browser's return URL, and the
  webhook handler is idempotent (a `payments` row per provider payment id with
  a unique constraint).
- Tricky: the hold runs out while the buyer is on the provider's page. The
  hold must be extended to cover the payment session's lifetime when the
  session is created, so a webhook never arrives for seats that were sold to
  someone else. If one does anyway (provider delays), the order is refunded
  automatically and the buyer told, instead of overselling.
- Tricky: two sessions for one order (back button, second tab). Reuse the open
  session; a second successful payment for an already paid order is refunded
  at once.
- Refunds from item 8 go through the provider.
- Undecided: which provider. I would build the interface and one provider and
  leave others to whoever needs them.

### 11. Sending email for real

`Notifier` writes every email to `outbox_messages`; nothing sends them. For
real use the tickets have to arrive.

- A sender job that drains the outbox over SMTP (`spring-boot-starter-mail`),
  with `sent_at`, `attempts` and `last_error` columns and a backoff.
- At least once, never in a tight loop: a message is claimed with
  `FOR UPDATE SKIP LOCKED`, sent, then marked. A crash between sending and
  marking sends it twice; that is acceptable for a ticket email, and the
  alternative loses tickets.
- The outbox page shows sent, waiting and failed messages, with a retry.
- Without SMTP settings the app behaves as today.

### 12. Throttled admin sign-in

The form login has no limit on attempts. Anyone can try passwords for
`admin@example.com` as fast as the server answers.

- Count failures per account and per address in memory (the `RateLimiter`
  from item 5), and after five failures in a few minutes answer with a delay
  and the same "wrong email or password" message.
- No permanent lockout: it would let anyone lock the organiser out the night
  of the event.
- Tests: in `admin/AdminLoginTest`.

### 13. Limits on public forms that send email

"Find my tickets" and "join the waitlist" send an email to whatever address is
typed. A script can use them to fill a stranger's inbox from the organiser's
address, which also hurts the organiser's sender reputation once item 11
sends for real.

- At most a few emails per address and per client per hour, refused quietly
  (the find page already says the same thing whether or not orders exist).
- Waitlist join for an address that never bought confirms the address first:
  the entry is created as `UNCONFIRMED` and only queues after the link in the
  email is opened. Undecided; it adds a step for honest people.

### 14. Door staff accounts

Every account is a full admin. The volunteer scanning tickets can also
refund orders, change prices and add admins.

- A role on `AdminUser`: `ADMIN` or `DOOR`. Door accounts see the check-in
  page and the attendee list of upcoming events and nothing else.
- `SecurityConfig` rules per path, and the navigation in
  `fragments/admin.html` hides what the role cannot use.
- Tests: `admin/AdminSecurityTest` gets a door account hitting each admin
  area.

### 15. Scanner tokens for the door API

Scanner apps call `/api/v1/admin/**` with an admin's email and password in
HTTP Basic. The password lives on a phone at the door.

- Per-device tokens created by an admin, limited to the door endpoints and
  optionally to one event, shown once, stored hashed, revocable, with a last
  used time.
- Basic auth keeps working for a release, then goes.
- Depends on 14 for the door role the token carries.

### 16. Audit log of admin actions

Who cancelled that order, who changed the price, who un-did that check-in?
Only `checked_in_by` and the order note answer anything today.

- An `audit_entries` table written by the services (not controllers):
  admin, action, target, a short detail, time. A page per event and a filter
  per admin.
- Never deleted with the event; kept as plain text.

### 17. Scheduled backups and a tested restore

The README says nothing about backups. The Postgres volume holds every order.

- A `backup` service in `docker-compose.yml` running `pg_dump` on a schedule
  into a mounted folder, keeping the last N dumps.
- A section in the README with the restore command, and a script that
  restores the newest dump into a scratch database and checks the row counts,
  so the restore is tried and not just written down.

### 18. Upgrade guide and a migration check

There is one release. Upgrading means pulling the new image and letting
Flyway migrate on start, which is not written anywhere.

- `docs/upgrading.md`: back up first (item 17), what a failed migration looks
  like and how to go back.
- A test on PostgreSQL (item 6) that migrates a database seeded at the last
  release's schema to the current one, so a migration that only works on H2
  is caught.

### 19. Scheduled jobs with several instances

The architecture notes say one instance is expected and the jobs are
idempotent. Expiring holds twice is harmless, but two instances could both
send a reminder before either marks `reminder_sent_at`, and both run the
waitlist offers.

- Wrap each job in a PostgreSQL advisory lock (`pg_try_advisory_lock`) so
  only one instance runs it at a time; without it the job is skipped.
- `ReminderService` marks and sends in one transaction per order.

### 20. Metrics for operators

Only `health` and `info` are exposed. An organiser behind a busy on-sale
cannot see holds, expiries or payment errors.

- Micrometer counters and gauges: checkouts started, paid, expired, released,
  seats held right now, check-ins per minute, outbox backlog. A Prometheus
  endpoint behind a setting, off by default.

### 21. Accessibility pass

The checkout countdown (`static/js/countdown.js`) changes every second with
no announcement: a screen reader user does not hear that the hold is ending
and finds the pay button disabled. The check-in page's result is shown by
colour and a beep.

- `aria-live` announcements at a few points (5 minutes, 1 minute, expired),
  not every second.
- Field errors linked with `aria-describedby`, focus moved to the first error
  or the page message after a failed submit.
- Check-in results with text and an icon, not colour alone; contrast checked
  in both themes.
- Tests: MockMvc assertions on the attributes in the rendered pages.

### 22. Complimentary tickets and a guest list

Organisers give tickets to the band, press and friends. Today that means a
100% promo code and a fake checkout.

- An admin form that issues free tickets in a tier to a name and email,
  as a `PAID` order with total zero and a `complimentary` flag.
- Counts against the tier and capacity under the event lock, like any sale;
  refused when there is no room (item 4's message).
- The report shows comps separately from sales.

### 23. Selling at the door

Walk-up buyers pay cash or card at the table. The check-in page could sell
the remaining seats and admit the buyer in one step.

- Uses item 22's admin-created orders with a price and a payment method
  (`cash`, `card at door`), checked in on creation.
- The same lock and availability rules as online checkout, so a walk-up sale
  never takes the seat of an online buyer who is paying at that moment.
- The report splits door sales by method for cashing up.

### 24. Buyers passing a ticket on

Only an admin can rename a ticket (`OrderAdminService.renameTicket`). A buyer
who gives a ticket away has to email the organiser.

- From the ticket page, the buyer enters a new name and email. A new code is
  issued and the old one stops working, so the old QR code cannot be used as
  well.
- Not after check-in, not after the event started, and logged (item 16).

### 25. Questions at checkout

Workshops need dietary needs or a T-shirt size per attendee.

- Questions per event (text, choice, yes/no; required or not; per order or
  per ticket), answered on the checkout page, shown in the attendee list and
  exported in the CSV.
- Answers are personal data and follow item 31.

### 26. Hidden tiers behind an access code

Presales for members, a crew tier, a discounted student tier. A promo code
lowers the price of a visible tier; it cannot unlock a tier nobody else sees.

- A tier can be hidden; an access code on the event page reveals it for that
  visit. Availability and capacity work as for any tier.

### 27. Tiers that take several seats

The demo has table seats sold one by one. A table of four is one purchase
that takes four seats of the capacity.

- `seats_per_unit` on the tier. Availability and capacity count seats, the
  order counts units, check-in issues one ticket per seat.
- Tricky: every count in `OrderItemRepository` and `TierAvailability` has to
  multiply, and the waitlist asks for units. A mistake here oversells, so the
  tests compare unit and seat counts on every path.

### 28. Booking fees and tax on orders

Prices are final today. Many organisers add a booking fee or need VAT shown.

- A fee per ticket or per order, set per event, shown as a line in the
  summary and stored on the order. Tax shown as included, per event rate.
- Promo discounts apply to tickets, not fees; the report separates them.

### 29. Buyers cancelling before a deadline

An event may allow cancellations until a few days before. Today the buyer
has to ask.

- A per-event deadline; before it, the ticket page has "Cancel my order",
  which cancels, records the refund (item 8) and releases the seats to the
  waitlist.
- Undecided: refunds are only recorded until item 10 can send them.

### 30. Event series

A weekly quiz night is created again each week with "Duplicate"
(`EventCopier`). A series would create the dates at once and share the
description and tiers.

- Create N dates from one event with a rule (weekly, on chosen dates). Each
  date stays a normal event with its own capacity, sales and check-in.
- A series page listing the dates with what is left.

### 31. Forgetting buyers after an event

Names and emails of every buyer and waitlist entry stay forever.

- A setting for how long after an event personal data is kept. A job replaces
  names and emails in orders, tickets, waitlist entries and outbox messages
  with placeholders and keeps the numbers the report needs.
- An admin action to forget one buyer on request.

### 32. Sales across all events

The report is per event. An organiser wants the month at a glance.

- A page with sales, refunds and check-ins per event for a chosen period, and
  the same as CSV. Sums of the per-event figures, so the numbers match.

## Maybe

### 33. Pay what you want

A tier with a minimum price where the buyer chooses the amount. Common for
community events. The amount has to be checked against the minimum in the
service, and every report that multiplies quantity by unit price keeps
working because the price is stored per order item already.

### 34. Webhooks for orders and check-ins

A POST to a configured URL when an order is paid or cancelled or a ticket is
checked in, signed, retried from a table like the outbox. Useful for mailing
lists and spreadsheets; I do not know anyone who needs it yet.

### 35. An embeddable ticket widget

An iframe page with an event's tiers for the organiser's own site. The
content security policy has `frame-ancestors 'none'`, so the widget path
needs its own policy with an allowlist of sites, and the checkout should open
in a new tab rather than inside the frame.

### 36. Translations

All text is in English in the templates and in `BusinessException` messages.
Moving it to message bundles touches every page and service. Worth it only
when someone runs events in another language.

### 37. Reserved seating with a seat map

Theatres sell seat B12, not "a seat". This changes holds from counts to
specific rows: every seat is a row, holds lock seats, and two buyers must
never get B12. It is a different product from what the app is today, so it
stays here.

### 38. Offline check-in for scanner apps

Door wifi fails. A scanner app could download the event's codes and check in
offline, then sync. Two offline phones can admit the same ticket and only
learn it at sync, which item 1 cannot prevent. Worth it only with the
conflicts shown clearly afterwards. Depends on 1 and 15.

### 39. Wallet passes

Apple Wallet and Google Wallet passes for each ticket. Needs signing
certificates and developer accounts; the QR code page works on every phone
already.

## Shipped

### 1. Check-in that cannot admit a ticket twice

A scan is now one conditional update (`TicketRepository.markCheckedIn`): it
sets `checked_in_at` only where it is empty and the order is still `PAID`, and
`CheckInService` reports `ALREADY_CHECKED_IN` or `CANCELLED` from the reloaded
ticket when the update changed nothing. Scanning, the attendee list and the
door API all go through it, and undo is the reverse update. The
already-checked-in message now names the staff member as well as the time, as
planned. The scan also takes a shared lock on the order first, so a
cancellation cannot slip in between. `CheckInRaceTest` runs ten scanners at
once on a real PostgreSQL (`support/PostgresTest`, enabled by
`TEST_DATABASE_URL`, which CI now sets with a PostgreSQL service): the old code
admitted all ten, the new one admits one. The demo seeder sets its past arrival
times through the same update. Item 6 is left with the checkout races.

### 2. Promo code limits that hold under load

Applying a code now finds and locks it in one query
(`PromoCodeRepository.lockByEventIdAndCode`) before counting uses. Changing a
code's limits or switching it off in the admin area takes the same lock, and
`PromoCode` has a `version`, so an admin change can neither lose nor be lost. Payment checks that the code is still active; if not, the discount
is removed for good (the exception is excluded from rollback), nothing is
paid, and the checkout page shows the new total with the buyer's details kept.
A code that expires during the hold is honoured, as planned. The tier check at
payment was left out: a code's tier and amount cannot be edited after it is
created, so they cannot change under a hold. `PromoRaceTest` runs ten buyers
against a two-use code on PostgreSQL: without the lock seven got it, with it
two. A second race has an admin lowering the limit and switching the code off
while buyers apply.

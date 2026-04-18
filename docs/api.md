# JSON API

A small JSON API sits next to the HTML pages. All responses are UTF-8 JSON,
times without an offset are wall-clock times in the event's `timeZone`, and
instants (`checkedInAt`) are UTC ISO-8601 strings. Money is always in whole
cents of the event currency.

Errors use the matching HTTP status and a body like:

```json
{ "error": "Not found" }
```

## Public endpoints

### `GET /api/v1/events`

Upcoming published events, soonest first.

```json
[
  {
    "slug": "summer-jazz-night",
    "title": "Summer Jazz Night",
    "venue": "The Attic",
    "startsAt": "2026-06-12T20:00:00",
    "endsAt": "2026-06-12T23:30:00",
    "timeZone": "Europe/Bucharest",
    "status": "PUBLISHED"
  }
]
```

### `GET /api/v1/events/{slug}`

One event with its ticket tiers and how many tickets are left. Cancelled events
are still returned (with `"status": "CANCELLED"`); drafts are `404`.

```json
{
  "event": { "slug": "summer-jazz-night", "title": "Summer Jazz Night", "...": "..." },
  "description": "Three sets, two bands.",
  "venueAddress": "Str. Lipscani 12, Bucharest",
  "tiers": [
    { "id": 4, "name": "Standing", "priceCents": 4500, "currency": "RON", "remaining": 37, "soldOut": false }
  ]
}
```

## Door endpoints

For scanner apps used by door staff. These need an admin account, sent with
HTTP Basic authentication on every request (the API keeps no session):

```bash
curl -u door@example.com:door-password \
  -H 'Content-Type: application/json' \
  -d '{"code": "K7M2Q9X4P3TZ"}' \
  http://localhost:4411/api/v1/admin/events/3/check-in
```

Missing or wrong credentials get `401 Unauthorized`. Create a separate admin
for door staff under **Admins** so their check-ins are recorded under their
own name.

### `POST /api/v1/admin/events/{eventId}/check-in`

```json
{ "code": "K7M2Q9X4P3TZ" }
```

The code may contain spaces, dashes or lower case letters. The response says
whether to let the person in, plus the running count for the event:

```json
{
  "status": "CHECKED_IN",
  "admitted": true,
  "attendee": "Ana Pop",
  "tier": "Standing",
  "checkedInAt": "2026-06-12T17:41:09Z",
  "checkedIn": 113,
  "total": 140
}
```

`status` is one of `CHECKED_IN`, `ALREADY_CHECKED_IN`, `NOT_FOUND`,
`WRONG_EVENT` or `CANCELLED`. Only `CHECKED_IN` has `"admitted": true`.

### `GET /api/v1/admin/events/{eventId}/attendees?q=&page=1`

Valid tickets of the event by attendee name, 25 per page. `q` matches part of a
name or email, or a whole ticket code or order reference.

```json
{
  "items": [
    { "code": "K7M2Q9X4P3TZ", "name": "Ana Pop", "email": "ana@example.com", "tier": "Standing", "order": "ET-7K3F9Q", "checkedInAt": null }
  ],
  "page": 1,
  "totalPages": 6,
  "total": 140
}
```

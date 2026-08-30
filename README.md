# event-tickets

Ticketing for small venues and meetups: publish an event, sell a few kinds of
tickets, and let people in at the door.

- **Events** with ticket tiers, prices, quantities and sales windows
- **Checkout** that holds the chosen seats for 10 minutes while the buyer fills
  in their details (payment is simulated)
- **Promo codes** with percentage or fixed discounts, usage limits and expiry
- **Tickets** with QR codes on the buyer's confirmation page, emailed as a link
- **Waitlist** for sold-out tiers: released tickets are offered in the order
  people joined
- **Door check-in** by scanning QR codes with a phone camera or USB scanner
- **Attendee list** with search and CSV export
- **Sales report** per event: tiers, revenue, promo codes and sales by day
- **JSON API** for event listings and door scanner apps, see [docs/api.md](docs/api.md)

## Stack

| Part     | Tech                                                 |
| -------- | ---------------------------------------------------- |
| App      | Java 21, Spring Boot 3.5, Spring MVC, Spring Security |
| Data     | PostgreSQL, Spring Data JPA, Flyway migrations       |
| UI       | Thymeleaf templates with htmx                        |
| Tests    | JUnit 5, Spring Boot Test, MockMvc, H2 (Postgres mode) |

## Running with Docker

```bash
docker compose up --build
```

Open http://localhost:4411. The admin area is at http://localhost:4411/admin;
sign in with the demo account:

- **Email:** `admin@example.com`
- **Password:** `demo-password`

On first start the app creates a few demo events with orders, promo codes
(`FRIENDS10`, `BAND`), a sold-out film night with a waitlist and a past event
with check-ins. Set `SEED_DEMO=false` to start with an empty database.

There is no mail server: emails the app would send (tickets, waitlist offers)
can be read under **Outbox** in the admin area.

## Local development

You need Java 21 and a PostgreSQL database. The quickest way to get one is to
start only the database from the compose file:

```bash
docker compose up -d db
ADMIN_EMAIL=admin@example.com ADMIN_PASSWORD=change-me ./mvnw spring-boot:run
```

The app listens on http://localhost:8080 and applies database migrations on
startup.

## Configuration

All settings are environment variables (or the matching `app.*` /
`spring.*` properties in `application.yml`).

| Variable                  | Default                                  | What it does                                                    |
| ------------------------- | ---------------------------------------- | --------------------------------------------------------------- |
| `DATABASE_URL`            | `jdbc:postgresql://localhost:5432/tickets` | JDBC URL of the Postgres database                             |
| `DATABASE_USER`           | `tickets`                                | Database user                                                   |
| `DATABASE_PASSWORD`       | `tickets`                                | Database password                                               |
| `PORT`                    | `8080`                                   | HTTP port inside the container / on your machine                |
| `BASE_URL`                | `http://localhost:8080`                  | Public address, used for links in emails                        |
| `ADMIN_EMAIL`             | _(empty)_                                | First admin account, created when there are no admins           |
| `ADMIN_PASSWORD`          | _(empty)_                                | Password of that account                                        |
| `SEED_DEMO`               | `false`                                  | Fill an empty database with demo events and orders              |
| `CHECKOUT_HOLD_DURATION`  | `10m`                                    | How long seats are held while a buyer checks out                |
| `WAITLIST_OFFER_DURATION` | `24h`                                    | How long released tickets are held for someone on the waitlist  |
| `JAVA_OPTS`               | see `Dockerfile`                         | JVM options for the container                                   |

Docker Compose also reads `WEB_PORT` (default `4411`) and `DB_PORT`
(default `5432`) for the ports published on your machine. Copy
`.env.example` to `.env` to change any of these without editing the compose
file.

## Tests

```bash
./mvnw verify
```

Integration tests boot the whole application against an in-memory H2 database,
so no running Postgres is needed. `./mvnw spotless:apply` formats the code;
`verify` fails on unformatted code.

## Running behind a reverse proxy

Put nginx, Caddy or Traefik in front for HTTPS and point it at the app port.
The app understands `X-Forwarded-Proto` and `X-Forwarded-For`, so redirects
stay on https. Set `BASE_URL` to the public address. A minimal nginx block:

```nginx
location / {
    proxy_pass http://127.0.0.1:4411;
    proxy_set_header Host $host;
    proxy_set_header X-Forwarded-Proto $scheme;
    proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
}
```

Only `/actuator/health` and `/actuator/info` are exposed; block `/actuator` at
the proxy if you do not want them public.

## Troubleshooting

**The app does not start: `Connection to localhost:5432 refused`.** Postgres is
not running or `DATABASE_URL` points elsewhere. With Docker:
`docker compose up -d db`.

**I cannot sign in to the admin area.** The first admin is only created when
there are no admins at all. If you changed `ADMIN_PASSWORD` after the first
start, the old password still applies; remove the `db-data` volume
(`docker compose down -v`) to start over, or add another admin under
**Admins**.

**Links in emails point to localhost.** Set `BASE_URL` to the address people
use to reach the app.

**Tickets show as sold out right after somebody left the checkout.** Seats are
held for 10 minutes (`CHECKOUT_HOLD_DURATION`). If people are on the waitlist,
released seats are offered to them first and held for 24 hours
(`WAITLIST_OFFER_DURATION`).

**The camera button is missing on the check-in page.** Camera scanning needs a
browser with the BarcodeDetector API (Chrome, Edge, most Android browsers) and
HTTPS (or localhost). Typing the code or using a USB scanner always works.

## Project layout

```
src/main/java/com/sorinwork/tickets/
  event/      events, ticket tiers, calendar files
  order/      checkout holds, orders, availability, reminders
  promo/      promo codes and discounts
  ticket/     tickets, QR codes, check-in, attendee list
  waitlist/   waitlist and offers of released tickets
  notify/     outbox and buyer emails
  report/     sales report
  admin/      admin pages and accounts
  web/        public pages
  api/        JSON API
  config/     security, scheduling, properties
  seed/       demo data
src/main/resources/
  db/migration/   Flyway migrations
  templates/      Thymeleaf pages (public/, admin/, fragments/)
  static/         CSS and the little JavaScript there is
src/test/java/    unit and integration tests
docs/             API and architecture notes
```

## License

MIT, see [LICENSE](LICENSE).

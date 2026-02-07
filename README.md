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

## Tests

```bash
./mvnw verify
```

Integration tests boot the whole application against an in-memory H2 database,
so no running Postgres is needed. `./mvnw spotless:apply` formats the code;
`verify` fails on unformatted code.

## License

MIT, see [LICENSE](LICENSE).

# event-tickets

Ticketing for small venues and meetups: publish an event, sell a few kinds of
tickets, and let people in at the door.

- **Events** with ticket tiers, prices, quantities and sales windows
- **Checkout** that holds the chosen seats for 10 minutes while the buyer fills
  in their details (payment is simulated)
- **Promo codes** with percentage or fixed discounts, usage limits and expiry
- **Tickets** with QR codes on the buyer's confirmation page

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
the first admin account is created from `ADMIN_EMAIL` and `ADMIN_PASSWORD`
(defaults in `docker-compose.yml`).

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

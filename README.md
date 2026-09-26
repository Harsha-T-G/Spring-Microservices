# Mini Order and Inventory System

Two independently built Java 21 / Spring Boot 3.5.16 applications demonstrate
synchronous service communication with Spring RestClient. Inventory owns stock
and atomic reservations. Order owns orders and calls Inventory through its own
HTTP contract. Both services use one PostgreSQL database, with separate `inventory` and
`orders` schemas. Each owns its domain types and Flyway migration history.

Implementation is on `feat/independent-services`; the initial `main` remains the
documentation/toolchain foundation. See [verification evidence](docs/test-evidence.md)
for actual results and [acceptance criteria](docs/spec.md) for scope.

## Build and test

Install JDK 21 and set `JAVA_HOME` to it. Python 3 is needed only for the optional
end-to-end verification script; Docker is needed for the PostgreSQL-backed tests and local containers.
The checked-in Maven 3.9.16 wrappers download Maven and dependencies on first use.

Run from the repository root:

```sh
java -version
(cd inventory-service && ./mvnw clean verify)
(cd order-service && ./mvnw clean verify)
```

Each service can build without the other service or its source. Tests use MockMvc
with real local components. Order tests replace only the remote Inventory HTTP
boundary with WireMock. No live Inventory instance is required for its suite.

Focused examples:

```sh
(cd inventory-service && ./mvnw -Dtest=InventoryApiTest test)
(cd order-service && ./mvnw -Dtest=OrderApiTest test)
```

## Run the services

From the repository root, create your local credentials file from the tracked
example. Edit `DB_USER` and `DB_PASSWORD` in `.env` if you want different local
values. Docker Compose reads `.env` automatically and passes the same pair to
the PostgreSQL container and both Spring services. `.env` is Git-ignored;
the example credentials are for local development only. Prebuilt JARs are not
needed:

```sh
cp -n .env.example .env
docker compose down --remove-orphans
docker compose up --build -d
docker compose ps
docker compose logs -f
```

Wait for `http://localhost:8081/actuator/health` and
`http://localhost:8080/actuator/health` to return UP before sending requests.
Stop without deleting data using `docker compose down`. The named volume keeps
stock, reservations and orders across application and container restarts.
`docker compose down -v` removes the new demo database and its history.
If you change credentials after a database volume has been initialized, update
the existing database role as well; editing `.env` alone does not change it.

For separate local Java processes, build both JARs and start only PostgreSQL:

```sh
docker compose up -d postgres-db
(cd inventory-service && ./mvnw clean package)
(cd order-service && ./mvnw clean package)
```

Then run each command in a separate terminal from the repository root.
Sourcing `.env` is required in each Java terminal when it contains custom
credentials:

```sh
set -a; source .env; set +a
java -jar inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

```sh
set -a; source .env; set +a
java -jar order-service/target/order-service-0.0.1-SNAPSHOT.jar
```

Inventory listens on 8081 and Order on 8080. The local JDBC defaults target
the same PostgreSQL database on 127.0.0.1 port 5433 with the example credentials.
Docker Compose reads `.env` automatically; standalone `java -jar` processes do
not, so export `DB_USER` and `DB_PASSWORD` when using custom values. For
anything beyond local development, supply `DB_URL`, `DB_USER` and `DB_PASSWORD`
from your runtime environment.
Only the explicit Inventory `dev` profile seeds JAVA-BOOK=20,
KEYBOARD-01=10 and MONITOR-24=5, and Flyway applies that seed once per database.
The default Inventory profile has no demonstration stock. Use Ctrl+C to stop
local processes; `docker compose down` stops the databases.

Compose publishes the API and database ports on localhost and sets Order's
Inventory URL to `http://inventory-service:8081`. `ORDER_PORT`,
`INVENTORY_PORT` and `DB_PORT` override host ports.

## APIs and examples

| Service | Method / endpoint | Result |
| --- | --- | --- |
| Inventory | GET `/api/v1/inventory/{sku}` | Stock, case-insensitive SKU; 404 if absent |
| Inventory | POST `/api/v1/inventory/{sku}/reservations` | Atomic, idempotent reservation; 201 |
| Order | POST `/api/v1/orders` | CONFIRMED order; 201 + Location |
| Order | GET `/api/v1/orders/{id}` | Stored confirmed/rejected order; 404 if absent |
| Order | GET `/api/v1/orders` | Array sorted by creation time, then ID |
| Both | GET `/actuator/health`, `/actuator/info` | Local health and application name/version |

```sh
curl -i http://localhost:8081/api/v1/inventory/java-book
curl -i http://localhost:8080/api/v1/orders \
  -H 'Content-Type: application/json' \
  -H 'Idempotency-Key: customer-order-123' \
  -H 'X-Correlation-Id: demo-order-1' \
  -d '{"customerId":"CUST-1001","sku":"JAVA-BOOK","quantity":2}'
curl -i http://localhost:8080/api/v1/orders
curl -i http://localhost:8081/api/v1/inventory/JAVA-BOOK
```

Repeat the exact POST with the same key: it returns the original order and
stock stays at 18. Change the quantity with the same key to see 409. Use a new
key and quantity 999 to see a stored REJECTED order and HTTP 422. Retrieve an
order using the path in its Location header, or the `orderId` in a rejection.

Keys are required, case-sensitive, 1–128 letters/digits/`._:-`. Customer IDs are
trimmed and case-sensitive; SKUs are trimmed and normalized to uppercase.
Quantity must be a positive JSON integer. Correlation accepts a single value
of 1–64 letters/digits/`._-`, otherwise generates a UUID. Errors and response
headers use the current request's ID. Full payloads and examples are in the
[API specification](docs/api-specification.md).

## Try the APIs with Swagger UI

Start both services using the commands above, with Inventory's `dev` profile.

| Service | Swagger UI | OpenAPI JSON |
| --- | --- | --- |
| Order | http://localhost:8080/swagger-ui.html | http://localhost:8080/v3/api-docs |
| Inventory | http://localhost:8081/swagger-ui.html | http://localhost:8081/v3/api-docs |

1. Open Order Swagger UI, expand **POST /api/v1/orders**, and click **Try it out**.
2. Enter a new `Idempotency-Key`, for example `swagger-order-1`. Optionally set
   `X-Correlation-Id` to `swagger-demo-1` to follow both service logs.
3. Use the supplied body example (`CUST-1001`, `JAVA-BOOK`, quantity `2`) and execute.
4. Open Inventory Swagger UI and GET `JAVA-BOOK` to see stock decrease from 20 to 18.
5. Repeat the identical Order POST with the same key to confirm replay without
   another stock decrease. Use a new key for each genuinely new order.

Inventory's direct reservation operation includes a sample UUID. Direct
reservations change real PostgreSQL stock independently of Order, so prefer
Order's POST for the complete application flow. Documented errors show the
actual API status codes and safe error schema. No authorization token is needed.

Swagger UI and OpenAPI are enabled for this learning application. To disable
both, set `springdoc.api-docs.enabled=false` and
`springdoc.swagger-ui.enabled=false`. Only `/api/v1/**` business operations are
documented; Actuator exposure remains health/info only. Springdoc 2.9.1 is the
Spring Boot 3 generation described in the [official documentation](https://springdoc.org/v2/).

## Configuration and failure behavior

Override YAML values with Spring command-line options or environment variables.
For example `--inventory.response-timeout-ms=750` or
`INVENTORY_RESPONSE_TIMEOUT_MS=750`. Inventory's base URL also supports
`INVENTORY_BASE_URL`. Values outside the validated bounds fail startup.

| Property | Default | Rule |
| --- | --- | --- |
| `server.port` | Order 8080 / Inventory 8081 | Independent service port |
| `inventory.base-url` | `http://localhost:8081` | Order's dependency URL |
| `inventory.connect-timeout-ms` | 500 | 1–500 ms |
| `inventory.response-timeout-ms` | 1000 | 1–1000 ms |
| `inventory.retry.max-attempts` | 2 | 1–2, including initial attempt |
| `inventory.retry.wait-ms` | 100 | 1–1000 ms |
| `inventory.breaker.open-wait-ms` | 5000 | Positive milliseconds |
| `orders.in-progress-wait-ms` | 4000 | 1–4000 ms for a matching request already processing |
| `spring.flyway.locations` | Schema; dev adds seed | Versioned migrations, no repeat seed on restart |
| `DB_URL`, `DB_USER`, `DB_PASSWORD` | Local Compose defaults | Shared database, separate JDBC pools and schemas |

The breaker wraps Retry: `CircuitBreaker(Retry(HTTP attempt))`. Its count-based
window and minimum calls are 4, failure threshold 50%, and half-open allowance
1 logical operation. Expected business rejections and key conflicts are ignored
by breaker statistics. A recovery trial can use the same two-attempt retry policy.

| Condition | Order response | Retry / stored result |
| --- | --- | --- |
| Valid reservation | 201 CONFIRMED | Store final order |
| Unknown SKU / insufficient stock | 422 with code and orderId | No retry; store REJECTED |
| Reused key with changed input | 409 | No retry or additional reservation |
| Invalid Order input | 400 | No Inventory call |
| Inventory 5xx / connection failure / timeout | 503 | At most one retry; retain attempt identity |
| Open circuit | 503 | Zero Inventory attempts |
| Unexpected Inventory 4xx / invalid success contract | 502 | No retry; no false rejection |
| Same key still processing after bounded wait | 503 ORDER_IN_PROGRESS | No concurrent reservation |

Successful and rejected replay return the original outcome. After a technical
failure, retry using the same key and body: the same order UUID is retained.
A timeout is an unknown outcome, not proof that Inventory did nothing.
Order health UP describes its local process, not Inventory connectivity.

## Evidence and demonstration

Run all real-service scenarios on temporary ports, with automatic cleanup:

```sh
python3 scripts/verify-e2e.py
```

The script uses `JAVA_HOME/bin/java` or `java` on PATH; `--java /path/to/java`
selects one explicitly. It creates one disposable PostgreSQL container and
verifies success, duplicate, rejection, correlation, Inventory shutdown,
circuit opening, recovery and persistence through both application restarts.
The script stops only its own temporary database container. Raw logs stay in
ignored `.local/e2e/`.

See [curated evidence](docs/test-evidence.md), the [ten-minute demonstration](docs/demo.md),
[service diagram](docs/diagrams/services.mmd), [success sequence](docs/diagrams/order-success.mmd),
and [failure sequence](docs/diagrams/inventory-unavailable.mmd). Source filenames
are also indexed in [service boundaries](docs/service-boundaries.md).

## Project workflow and limitations

Read [AGENTS.md](AGENTS.md), [CONTEXT.md](CONTEXT.md), the [capability map](CAPABILITY-MAP.md),
[task status](docs/plans/tasks.md), [project structure](docs/project-structure.md),
and [learning notes](docs/microservices-notes.md). Project-owned [SDD](.agents/skills/spec-driven-development/SKILL.md)
and [TDD](.agents/skills/test-driven-development/SKILL.md) skills are local, pinned
adaptations with [source provenance](docs/skill-provenance.md). Production Java
has no comments; domain models use Lombok with constructor invariants.

Use `feat/<reason>` for implementation and the [PR template](.github/pull_request_template.md)
for criterion → test → actual result evidence. Raw validation reports/logs remain
local. The implementation is published in [PR #1](https://github.com/Harsha-T-G/Spring-Microservices/pull/1); PostgreSQL-backed suites, real-process restarts,
and an isolated Compose smoke test have now passed. See [delivery notes](docs/delivery.md) for review-ready PR text.

This is a learning exercise: each service now persists its data and idempotency
history in PostgreSQL, but Order's request locks are process-local, so multiple
Order replicas require additional coordination. There is no stock release,
cross-service transaction, authentication, CSRF configuration, gateway,
discovery, messaging or frontend. Existing Boot
SLF4J/Logback provides logging; no extra logging stack is added. The default
latency check is an isolated transport measurement, not a whole-system SLA.
Boot 3.5.16 is the required Boot 3 exercise baseline and its final OSS release;
a deployed system needs a supported maintenance baseline. Container tags are
Java-major tags, not immutable deployment digests. The current database topology and production limits are explained in
[ADR 0004](docs/adr/0004-shared-postgresql.md).

The previous two-database Compose volumes are not automatically imported into
the new single database. `docker compose down --remove-orphans` stops old
containers but preserves their volumes. Keep those volumes if they contain
data you need; see [ADR 0004](docs/adr/0004-shared-postgresql.md).

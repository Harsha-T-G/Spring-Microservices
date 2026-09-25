# Mini Order and Inventory System

Two independently built Java 21 / Spring Boot 3.5.16 applications demonstrate
synchronous service communication with Spring RestClient. Inventory owns stock
and atomic reservations. Order owns orders and calls Inventory through its own
HTTP contract. Each service uses separate in-memory state and domain types.

Implementation is on `feat/independent-services`; the initial `main` remains the
documentation/toolchain foundation. See [verification evidence](docs/test-evidence.md)
for actual results and [acceptance criteria](docs/spec.md) for scope.

## Build and test

Install JDK 21 and set `JAVA_HOME` to it. Python 3 is needed only for the optional
end-to-end verification script; Docker Compose is needed only for containers.
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

After building, use separate terminals from the repository root:

```sh
java -jar inventory-service/target/inventory-service-0.0.1-SNAPSHOT.jar --spring.profiles.active=dev
```

```sh
java -jar order-service/target/order-service-0.0.1-SNAPSHOT.jar
```

Inventory listens on 8081 and Order on 8080. Only the explicit `dev` profile
seeds JAVA-BOOK=20, KEYBOARD-01=10, MONITOR-24=5. The default Inventory profile
starts with no demonstration stock. Stop processes with Ctrl+C.

Alternatively, after both builds:

```sh
docker compose up --build -d
docker compose logs -f
docker compose down
```

The images package the already-tested JARs on a Java 21 runtime and run as a
non-root numeric user. Compose publishes ports on localhost and sets Order's
Inventory URL to `http://inventory-service:8081`. Wait for each health endpoint
before sending demo orders; Compose does not imply dependency readiness.
`ORDER_PORT` and `INVENTORY_PORT` override host ports. No persistent volumes are
used. Container startup has not been executed in the recorded verification run;
Compose configuration validation and native Java process verification have.

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
| `inventory.initial-stock` | Empty outside dev | Inventory map of SKU to nonnegative stock |

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
selects one explicitly. It verifies success, duplicate, rejection, correlation,
Inventory shutdown, circuit opening and recovery. Inventory restart resets its
in-memory data; this limitation is explicitly included in the evidence. Raw logs
stay in ignored `.local/e2e/`.

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
local. No remote or published PR exists yet; publication is a separate remaining
step. See [delivery notes](docs/delivery.md) for review-ready PR text.

This is a learning exercise: state and idempotency history are lost on restart,
attempt/key maps have no eviction, and replicas would not share replay safety.
There is no stock release, persistence, distributed transaction, authentication,
CSRF configuration, gateway, discovery, messaging or frontend. Existing Boot
SLF4J/Logback provides logging; no extra logging stack is added. The default
latency check is an isolated transport measurement, not a whole-system SLA.
Boot 3.5.16 is the required Boot 3 exercise baseline and its final OSS release;
a deployed system needs a supported maintenance baseline. Container tags are
Java-major tags, not immutable deployment digests. Production improvements are
explained in the [design decision](docs/adr/0002-service-implementation.md).

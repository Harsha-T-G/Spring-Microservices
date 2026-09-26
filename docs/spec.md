# Specification and acceptance index

Status: implementation and automated verification completed on the feature
branch. Selected API defaults are recorded in the API baseline and ADR 0002.
See test-evidence.md for results; an isolated Compose runtime check passed.
A live presentation remains outside the completed checks. PR #1 is published.

[Capability map](../CAPABILITY-MAP.md), [API contract](api-specification.md),
[boundaries](service-boundaries.md), [structure](project-structure.md),
[chapter plan](implementation-plan.md), [tasks](plans/tasks.md).

## Foundation

| ID | Acceptance criterion |
| --- | --- |
| FND-001 | Two independent applications build/run on JDK 21 with compatible pinned Boot 3.x; each has its own pom and wrapper. |
| FND-002 | No shared application DTO/model/business module or cross-service storage access; ports/URL/timeouts use configuration. |
| FND-003 | Initial main contains only foundation/toolchain/docs; application edits start on feat/reason. |
| FND-004 | Local SDD/TDD skills, licenses and provenance work without remote invocation; domain models use Lombok and dependencies follow the minimal policy. |

## Inventory

| ID | Acceptance criterion |
| --- | --- |
| INV-001 | Dev seeds JAVA-BOOK=20, KEYBOARD-01=10, MONITOR-24=5; case-insensitive GET returns stock or 404. |
| INV-002 | Required key/orderId and positive integer quantity validated; invalid input is 400 without stock effects. |
| INV-003 | Successful reservation returns 201 with required fields and reduces stock once. |
| INV-004 | Unknown SKU is 404; insufficient stock is 409 with unchanged stock. |
| INV-005 | Matching key/request returns original successful reservation; changed order/SKU/quantity conflicts with 409. |
| INV-006 | Concurrent distinct reservations cannot oversell; matching-key requests decrement once; cross-SKU key reuse conflicts atomically. |

## Order

| ID | Acceptance criterion |
| --- | --- |
| ORD-001 | Valid order becomes CONFIRMED, returns 201 plus Location and order/reservation IDs. |
| ORD-002 | Inventory receives order ID, quantity, key and correlation ID through RestClientInventoryClient behind InventoryClient. |
| ORD-003 | Unknown SKU/insufficient stock yield clear rejection; selected stored REJECTED/422 behavior stays explicit in API spec. |
| ORD-004 | Unreachable Inventory yields controlled 503, never fabricated success. |
| ORD-005 | Completed matching-key replay returns original outcome without Inventory call; changed input returns 409. |
| ORD-006 | GET returns stored order or 404; list is ascending createdAt, empty array when empty. |
| ORD-007 | Selected uncertainty handling retains order ID/key across 503 retries and bounds same-key concurrency waits; technical uncertainty is not business rejection. |
| ORD-008 | Validation and dependency errors use consistent safe envelopes; HTTP details stay outside OrderService. |

## Resilience

| ID | Acceptance criterion |
| --- | --- |
| RES-001 | Real transport applies configurable connect timeout ≤500 ms and response timeout ≤1 s. |
| RES-002 | Connection failures/timeouts/5xx receive at most one additional attempt with unchanged key/orderId; 400/404/business 409 are not retried. |
| RES-003 | Repeated measured failures open breaker; open circuit returns 503 and makes zero remote requests. |
| RES-004 | After configured wait, healthy recovery trial closes circuit; failed trial reopens it. |
| RES-005 | Exhausted/timeout/connection failure yields controlled 503; retry logs and errors expose no bodies or internals. |
| RES-006 | Breaker nesting, thresholds, minimum calls and trial policy match documented and tested behavior. |

## Observability

| ID | Acceptance criterion |
| --- | --- |
| OBS-001 | Both services preserve valid ID and generate UUID for missing/invalid ID; headers/errors/logs agree. |
| OBS-002 | Order forwards the same ID on each attempt; Inventory logs/returns it. |
| OBS-003 | MDC clears on success/error; logs include service, method/path/status/duration and operation IDs when available. |
| OBS-004 | No bodies/secrets/expected-4xx stack traces; health/info only, with name/version and no false dependency-health claim. |

## Delivery

| ID | Acceptance criterion |
| --- | --- |
| DEL-001 | At least three authentic failing-to-passing cycles recorded; both independent suites verified on JDK 21. |
| DEL-002 | Real services demonstrate success, duplicate, insufficient stock, unavailable Inventory and recovery. |
| DEL-003 | Evidence proves timeout, retry count/key reuse, open-circuit zero calls, recovery and common correlation ID. |
| DEL-004 | README includes prerequisites/build/test/start/config/endpoints/curl/failure tables/limits; editable diagrams and learning notes cover the exercise. |
| DEL-005 | PR links specs/diagrams and actual results/limits, excludes disposable reports; ten-minute demo covers required behaviors. |

Detailed HTTP shapes and selected normalization/rejection/replay/locking choices
remain in the API contract rather than being duplicated here. Implemented
criteria and actual verification map to tests in [evidence](test-evidence.md).
DEL-005 remains partial until a live demo is delivered; PR #1 is published.

## API exploration update

User-requested extension: Swagger UI in both services and the three diagrams specified
in the exercise: service diagram, successful-order sequence, and Inventory-unavailable sequence.

| ID | Acceptance criterion |
| --- | --- |
| API-001 | Both services serve Swagger UI and OpenAPI JSON with service title/version and only their business API paths. |
| API-002 | POST operations expose required Idempotency-Key, optional X-Correlation-Id, valid example bodies, success and safe error schemas/statuses. |
| DOC-001 | Exactly the three required editable Mermaid diagrams remain; all render and documentation links/previews agree. |

## Persistence revision

The assignment permits in-memory storage; the user chose PostgreSQL and Flyway.
ADR 0003 records the design and supersedes earlier in-memory wording.

| ID | Acceptance criterion |
| --- | --- |
| DB-001 (superseded by DB-007) | The first persistence revision used separate PostgreSQL databases. |
| DB-002 | Inventory stock, reservations and replay results survive service restart; concurrent reservations never oversell or reuse a key for changed input. |
| DB-003 | Order attempts, stable IDs, final outcomes and replay survive service restart; technical failure retains the attempt for safe retry. |
| DB-004 | `dev` stock seed runs once per database; default profile remains unseeded; Compose uses durable volumes and starts without prebuilt JARs. |
| DB-005 | Tests run against isolated PostgreSQL and a real-process check demonstrates restart persistence. |
| DB-006 | A tracked `.env.example` documents one local database username/password; a Git-ignored `.env` supplies the same credentials to PostgreSQL and both services. |

## Shared database revision

The user selected one PostgreSQL database for Order and Inventory. Each service
keeps its own tables and Flyway history in a separate schema. This replaces the
physical database separation in DB-001 and ADR 0003 without changing the HTTP
contract or allowing cross-service table access.

| ID | Acceptance criterion |
| --- | --- |
| DB-007 | Compose runs one PostgreSQL container and one database; both services connect to it with one credential pair, using separate `inventory` and `orders` schemas and independent Flyway histories. |
| DB-008 | A real two-service run proves both migrations coexist in one database, Order can reserve Inventory stock over HTTP, and restart/replay behavior remains intact. |

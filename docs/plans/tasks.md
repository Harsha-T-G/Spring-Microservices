# Implementation slices

Application slices are implemented on feat/independent-services. Results are
recorded in [test evidence](../test-evidence.md); the published PR and remaining live demonstration are tracked separately.

| Task | Criteria | Slice and main files/responsibilities | Verification / status |
| --- | --- | --- | --- |
| 00 Foundation | FND-003/004 | Local skills/guidelines, specs, diagrams, PR/evidence template, initial main commit | Documentation/skill-metadata/source checks PASS; initial main intentionally has no application |
| 01 Independent scaffold | FND-001/002/004 | On feat/independent-services, replace starter with two Maven applications, Java 21/Boot 3.x, required dependencies and wrappers | Each independent clean verify and startup; PASS; see evidence |
| 02 Stock lookup | INV-001 | Inventory controller/service/store/config + InventoryApiTest | RED existing-stock GET, GREEN; separate missing/case-insensitive cases; PASS; see evidence |
| 03 Reservation | INV-002/003/004 | Request/response DTOs, domain models, stock operation, exception handling | One API behavior at a time; stock changes/error/no-effect checks; PASS; see evidence |
| 04 Inventory replay/races | INV-005/006 | Atomic reservation/key storage + coordinated API tests | Original result, conflict fields, same/different-key races; PASS; see evidence |
| 05 Correlation foundation | OBS-001/002/003 | Filters/MDC/error envelope in both services | Headers/errors/cleanup; propagation completed with Order; PASS; see evidence |
| 06 Order success/read | ORD-001/002/006/008 | Order API/service/store, InventoryClient/RestClient adapter, WireMock tests | Request contract, confirmation, Location, get/list; PASS; see evidence |
| 07 Rejection/replay | ORD-003/004/005/007 | Order outcomes, key identity and coordination | Selected 422 stored-rejection behavior; business rejection, stable identity after 503, zero-call replay; PASS; see evidence |
| 08 Timeout/retry | RES-001/002/005 | HTTP config, typed client errors, retry | Real transport delay/refusal/500 and nonretryable 4xx; PASS; see evidence |
| 09 Breaker/recovery | RES-003/004/006 | Explicit breaker/retry composition | Open zero-call behavior, timed trial/recovery; PASS; see evidence |
| 10 Observability finish | OBS-004 | Logs and Actuator info/health | Required fields/metadata, exposure, no false connectivity claim; PASS; see evidence |
| 11 Delivery | DEL-001/002/003/004/005 | Compose, actual evidence, README commands, demo and PR | Both suites and real-service scenarios PASS; Compose config and isolated runtime PASS; PR #1 published; live demo NOT RUN |

Each row contains several behaviors; apply one RED/GREEN cycle at a time, not
one giant test/implementation batch. Resolve only decisions affecting the active
slice. Existing user authorization persists; routine choices need no new approval.

Planned focused command from Inventory's directory:
`./mvnw -Dtest=InventoryApiTest test`. From Order's directory:
`./mvnw -Dtest=OrderApiTest test`. Select actual class names when they exist;
see evidence for the commands executed. Both services passed `./mvnw clean verify`
on native JDK 21.

Suggested PR sequence: foundation, independent-services, inventory-reservations,
order-restclient, resilience, observability-and-demo. Use feat/reason for the
application branches. A PR may include several completed behavior slices when
they form one coherent review; do not create a PR merely to meet a count.

## Swagger and diagram follow-up

API-001/002: springdoc WebMVC UI in both services, generated API schemas,
request examples, headers and documented response codes.
DOC-001: keep the three diagrams named by the original exercise; remove only
the additional development-workflow diagram. Two sequence parser failures
were reproduced and fixed by replacing semicolons inside message text.
See the latest verification entry in test-evidence.md for executed checks.

## PostgreSQL and Flyway revision

| Slice | Criteria | Observable test and implementation | Status |
| --- | --- | --- | --- |
| 12 Inventory persistence | DB-001/002/004 | PostgreSQL migration and dev seed; API stock/replay/concurrency tests run against PostgreSQL | PASS; see PostgreSQL evidence |
| 13 Order persistence | DB-001/003 | Durable attempt before RestClient call and completed outcome; API replay/retry tests against PostgreSQL | PASS; see PostgreSQL evidence |
| 14 Deployment and restart | DB-004/005 | Compose two databases, multi-stage images, real-process restart and repeat-key checks | PASS; see PostgreSQL evidence |
| 15 Local database credentials | DB-006 | Add ignored `.env`, tracked example, and consistent Compose username/password interpolation | PASS; see local credentials evidence |

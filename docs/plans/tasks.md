# Implementation slices

Foundation documents are prepared; application work has not begun. A task is
complete only when its observable checks pass and evidence is recorded.

| Task | Criteria | Slice and main files/responsibilities | Verification / status |
| --- | --- | --- | --- |
| 00 Foundation | FND-003/004 | Local skills/guidelines, specs, diagrams, PR/evidence template, initial main commit | Documentation/skill-metadata/source checks PASS; application checks NOT RUN |
| 01 Independent scaffold | FND-001/002/004 | On feat/independent-services, replace starter with two Maven applications, Java 21/Boot 3.x, required dependencies and wrappers | Each independent clean verify and startup; NOT STARTED |
| 02 Stock lookup | INV-001 | Inventory controller/service/store/config + InventoryApiTest | RED existing-stock GET, GREEN; separate missing/case-insensitive cases; NOT STARTED |
| 03 Reservation | INV-002/003/004 | Request/response DTOs, domain models, stock operation, exception handling | One API behavior at a time; stock changes/error/no-effect checks; NOT STARTED |
| 04 Inventory replay/races | INV-005/006 | Atomic reservation/key storage + coordinated API tests | Original result, conflict fields, same/different-key races; NOT STARTED |
| 05 Correlation foundation | OBS-001/002/003 | Filters/MDC/error envelope in both services | Headers/errors/cleanup; propagation completed with Order; NOT STARTED |
| 06 Order success/read | ORD-001/002/006/008 | Order API/service/store, InventoryClient/RestClient adapter, WireMock tests | Request contract, confirmation, Location, get/list; NOT STARTED |
| 07 Rejection/replay | ORD-003/004/005/007 | Order outcomes, key identity and coordination | Resolve proposed status choices; business rejection, stable identity after 503, zero-call replay; NOT STARTED |
| 08 Timeout/retry | RES-001/002/005 | HTTP config, typed client errors, retry | Real transport delay/refusal/500 and nonretryable 4xx; NOT STARTED |
| 09 Breaker/recovery | RES-003/004/006 | Explicit breaker/retry composition | Open zero-call behavior, timed trial/recovery; NOT STARTED |
| 10 Observability finish | OBS-004 | Logs and Actuator info/health | Required fields/metadata, exposure, no false connectivity claim; NOT STARTED |
| 11 Delivery | DEL-001/002/003/004/005 | Compose, actual evidence, README commands, demo and PR | Both suites and real-service scenarios; NOT STARTED |

Each row contains several behaviors; apply one RED/GREEN cycle at a time, not
one giant test/implementation batch. Resolve only decisions affecting the active
slice. Existing user authorization persists; routine choices need no new approval.

Planned focused command from Inventory's directory:
`./mvnw -Dtest=InventoryApiTest test`. From Order's directory:
`./mvnw -Dtest=OrderApiTest test`. Select actual class names when they exist;
do not claim these commands already run. Run each service's `./mvnw clean verify`
before delivery, on JDK 21.

Suggested PR sequence: foundation, independent-services, inventory-reservations,
order-restclient, resilience, observability-and-demo. Use feat/reason for the
application branches. A PR may include several completed behavior slices when
they form one coherent review; do not create a PR merely to meet a count.

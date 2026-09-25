# Verification evidence

This file is durable assignment evidence, not a disposable code-review report.
Only actual observations are recorded. Raw logs remain in ignored `.local/`.

## Foundation

The documentation foundation defines criteria, local skills, conventions,
diagrams and PR requirements. Validation results are recorded below after the
checks execute. No application implementation or test success is implied.

Foundation validation on 2026-09-25, before the initial commit:

- Both local skills passed skill-creator's `quick_validate.py` (exit 0, “Skill is valid!”).
- Markdown local links/anchors, fences and whitespace passed a document check.
- All three service `.mmd` sources matched their embedded Mermaid previews.
- Four upstream source/license files matched the SHA-256 values in provenance.
- Four editable Mermaid sources were inspected; no Mermaid renderer was executed.
- The supplied starter's non-IDE files were hashed before installation for
  preservation checking. No application files were changed by this validation.

Checks ran against the prepared documentation bundle in the planning workspace
before Git initialization. The skill validator initially could not start because
PyYAML was missing; installing PyYAML 6.0.3 in a validation-only local folder
allowed both checks to pass. This is not an application dependency or TDD RED.
No JDK/Maven/application run was needed or claimed for this documentation change.

## Application verification — 2026-09-25

Tests ran in an isolated local checkout on feat/independent-services, based on
starter commit `7b63c19ed0eb4c6ac56cced0e496abec2c497d6e`. Each cycle was a dirty
working-tree iteration; no pretend per-cycle commit IDs are assigned. The final
source snapshot is identified below and is copied byte-for-byte to the user's
repository. Initial main remains `908389859572073504f69a9ec68a1d5659fbf580`.

Native runtime: OpenJDK 21.0.12.1; Maven wrapper 3.9.16; Boot 3.5.16. The scoped
runner set JAVA_HOME to the installed JDK 21 and ran each service's own wrapper.
Raw run metadata/output stay in the verification checkout's ignored `.local/logs/`.
A local copy is preserved in the destination repository under
`.local/implementation-evidence-*/`; no raw logs are committed.

### Final independent builds

| Working directory | Executed command | Tests | Failures / errors / skipped | Result |
| --- | --- | --- | --- | --- |
| inventory-service | `./mvnw -q clean verify` | 39 | 0 / 0 / 0 | PASS, local run 22-inventory-clean-verify |
| order-service | `./mvnw -q clean verify` | 60 | 0 / 0 / 0 | PASS, local run 24-order-final-clean-verify |

Inventory source was unchanged after its final build. Order was rebuilt after
adding failure-operation logging. The real-process scenario below was then rerun
against these final JARs. Runtime archive inspection confirms Lombok and WireMock
are absent from executable dependencies; only Boot's existing logging stack is
present, with explicit Resilience4j libraries only in Order. Both JARs start
independently. Source convention, cross-service import, local document-link,
Mermaid preview parity and diff whitespace checks pass.

Final implementation snapshot SHA-256: `14530b9ec3bbe66f060640e89fc441e056e9153746de284bd876c2fccc4aad3d`.
This hashes the sorted manifest of relative path/content SHA-256 for service
files (excluding target), compose.yml and scripts/verify-e2e.py. Documentation
is excluded to avoid a self-referential hash; raw manifest stays local.

### Authentic RED → GREEN cycles

For each row, RED used `./mvnw -q -Dtest=Class#method test` in the indicated
service directory; GREEN ran the affected class or service suite as recorded.
Failure was observed before the corresponding production change. Full method
names below can be used as focused selectors.

| Criterion | Service / test | Observed RED | GREEN command / result |
| --- | --- | --- | --- |
| INV-003 | inventory / InventoryApiTest#givenAvailableStock_whenReserving_thenReservationIsCreatedAndStockDecreases | Expected 201, got 404 | InventoryApiTest PASS; reservation and decrement implemented |
| INV-004 | inventory / InventoryApiTest#givenInsufficientStock_whenReserving_thenConflictLeavesStockUnchanged | Expected 409, got 404 | InventoryApiTest PASS; insufficient-stock mapping and no-effect assertion |
| INV-005 | inventory / InventoryApiTest#givenCompletedReservation_whenReplayingAfterStockExhausted_thenOriginalResultIsReturned | Replay returned 409 instead of 201 | InventoryApiTest PASS; key lookup precedes stock check |
| ORD-005 | order / OrderApiTest#givenCompletedOrder_whenReplayingWithNewCorrelation_thenOriginalOrderWithoutRemoteCallIsReturned | Returned a different order/time | `./mvnw -q -Dtest=OrderApiTest test` PASS; original outcome and one remote call |
| RES-002 | order / OrderApiTest#givenInventoryServerFailure_whenCreatingOrder_thenExactlyTwoStableAttemptsReturnSafeUnavailable | Expected 2 remote attempts, observed 1 | OrderApiTest PASS; two requests with identical body/key and correlation |
| RES-003/004 | order / OrderApiTest#givenRepeatedDependencyFailures_whenCircuitOpens_thenNoRequestsUntilTimedRecovery | Expected open-circuit 503, got 201 | OrderApiTest PASS; zero-call open behavior and timed recovery |

Local log labels for these cycles are 03-reservation, 04-insufficient,
05-replay, 13-order-replay, 14-retry and 15-breaker (`-red`/`-green`). Additional
cycles include stock lookup and cover missing-SKU envelopes, fingerprint conflicts, scalar validation,
Order read/list/rejection and domain constructor invariants. Cycle 23 additionally
reproduced a missing stable order ID in failure logs, then passed the focused
OrderApiTest#givenUnknownReservationOutcome_whenInventoryFails_thenLogsRetainOrderIdentity
test after logging the reservation attempt identity. It also asserts no customer
ID or idempotency key appears in captured application logs. Boundary tests
that were already green when added are supplemental verification, not invented REDs.

Initial sandbox/Mockito attachment and Maven-cache failures were infrastructure
failures, not TDD RED. The first Order transport run hit HTTP/2 upgrade EOF; the
HTTP/1.1 rerun passed, then the missing GET endpoint was reproduced separately
before implementation. The first E2E script expected an enum constant instead
of Resilience4j's actual transition log text; its assertion was corrected and
the whole scenario rerun successfully. None of those failed runs are counted
as final verification success.

### Acceptance coverage

| Criteria | Actual tests / public observations |
| --- | --- |
| FND-001/002/004 | Independent clean builds, application startup, separate DTOs/stores, Lombok constructor tests, packaged dependency inspection |
| FND-003 | main foundation hash unchanged; implementation stays on feat/independent-services |
| INV-001 | InventoryApiTest lookup/missing cases; DevelopmentStockTest seeds all three products; DefaultStockTest has no demo stock |
| INV-002/003/004 | InventoryApiTest success, malformed/missing fields, numeric coercion/overflow, invalid keys, unknown SKU and insufficient stock |
| INV-005/006 | Exhausted-stock replay, all changed fingerprint fields, 24 concurrent same/distinct-key requests, two concurrent cross-SKU claims |
| ORD-001/002/006 | OrderApiTest success/Location, outbound JSON/headers, get/missing/list order |
| ORD-003/005/007 | Business rejection and replay; changed customer/SKU/quantity; retained UUID after 503; OrderConcurrencyTest bounds same-key waiting without parallel reservation |
| ORD-004/008, RES-001/002/005 | 500 twice, actual delayed response, stopped WireMock server, safe malformed/unexpected contract translation, validation with no remote call |
| RES-001/002 | InventoryConfigurationTest rejects excessive/nonpositive timeouts, excess attempts and invalid wait configuration |
| RES-003/004/006 | Timed opening/recovery, failed half-open trial, ignored repeated business rejections; exact WireMock request counts |
| OBS-001/002/003 | CorrelationFilterTest in each service: invalid/missing/duplicate headers, same-thread MDC cleanup including exceptions; API forwarding and safe errors; real shared-ID logs |
| OBS-004 | ActuatorTest in both services: name/version, health, env not exposed; Order health remains local during dependency outage |
| DEL-001/002/003/004 | Curated cycles, clean builds, actual real-service scenarios, executable verification script, README, Mermaid sources and learning notes |
| DEL-005 | PR text and ten-minute demo prepared; publication and live presentation remain NOT RUN |

### Timeout, retry and circuit observations

The slow WireMock response delays headers by 2500 ms. With the real JDK transport's
1000 ms response timeout, two attempts and 100 ms retry wait, the clean-run request
returned 503 in 2330 ms of monotonic request time in the final clean run. The assertion
requires 1.9–4 seconds and verifies exactly two HTTP requests with the same key
and correlation ID. This is actual waiting, not a mocked timeout exception. The last XML timing case reports 900.054 seconds
of wall-clock elapsed time across a clock discontinuity; its request timer and
1.9–4-second assertion use System.nanoTime. No suite wall-clock speed claim is made.

The 500 test sees exactly two identical reservation payloads and stable headers;
400, unknown-SKU 404 and business 409 receive one request only. Unexpected/malformed
contracts receive 502 without retry. A stopped WireMock server produced a safe
503 in 118 ms in the final clean run. This measures refusal, not a simulated
500, and does not claim to measure a black-holed connection's handshake timeout.

With test open-wait 500 ms, four failed logical operations produce eight HTTP
attempts. The open request adds zero calls. A healthy timed trial increases the
count to nine; the next normal order makes ten. A separate failed-trial test
increases eight to ten, then proves immediate zero-call rejection and a later
healthy trial at eleven. Business rejections do not open the breaker. No test
uses Thread.sleep or manipulates breaker state to claim elapsed-time evidence.

### Real services

Command from the repository root, after both builds:

```sh
python3 scripts/verify-e2e.py --java /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home/bin/java
```

The script uses temporary localhost ports, launches only its own two JAR processes,
stops Inventory, restores it, and cleans up all processes. Full logs stay in
ignored `.local/e2e/`. Selected actual command output:

```json
{"scenario":"independent-startup","order_port":52650,"inventory_port":52649}
{"scenario":"success","http":201,"stock":18,"order_id":"ae98bacd-b38f-4ab5-bb50-bed8a4cdc3d0","reservation_id":"1e3e48a9-734e-40ad-9d76-fc9ae478eb4a","seconds":0.194}
{"scenario":"duplicate","http":201,"same_order":true,"stock":18}
{"scenario":"insufficient-stock","http":422,"code":"INSUFFICIENT_STOCK","stock":18}
{"scenario":"correlation","id":"e2e-success","present_in_both_service_logs":true}
{"scenario":"unavailable","request":1,"http":503,"seconds":0.115}
{"scenario":"unavailable","request":2,"http":503,"seconds":0.113}
{"scenario":"unavailable","request":3,"http":503,"seconds":0.115}
{"scenario":"unavailable","request":4,"http":503,"seconds":0.004}
{"scenario":"circuit-open","http":503,"seconds":0.003,"new_retry_logs":0}
{"scenario":"recovery-after-inventory-restart","http":201,"stock":18,"order_id":"7da525da-1202-44b8-96fa-e8dd80a67b00"}
{"scenario":"result","status":"PASS","note":"Inventory restart resets its in-memory stock and reservation history."}
```

Inventory restart intentionally reseeds 20 and forgets reservation history. The
recovery order uses a key that never reached Inventory during the outage; final
stock is 18 from the new process, not a claim of persistence. The same-ID log
assertion uses both service names, correlation `e2e-success`, and reservation ID.
WireMock provides exact zero-outbound-call proof; the native open-circuit check
also verifies fast rejection and no additional retry log.

### Remaining limits

- Compose `config --quiet` passes. Container images/startup have NOT RUN;
  end-to-end runtime evidence uses native JDK 21 processes.
- Original diagram validation checked source/preview parity only; the Swagger follow-up below adds actual rendering.
- No coverage percentage, load-test claim or production latency SLA is asserted.
- PR #1 is published. A ten-minute demo is documented, but no live presentation is claimed.
- State/idempotency history remains in memory and is lost on restart; replicas,
  persistence, retention and reconciliation are outside this exercise.

## Swagger UI and diagram correction follow-up

Based on commit `6b3618d`, with edits tested in an isolated working copy and
transferred without changes to the implementation branch. Native JDK 21.0.12.1
and the same Maven wrappers were used. Springdoc WebMVC UI 2.9.1 is explicitly
requested for manual API exploration, following its [Boot 3 documentation](https://springdoc.org/v2/).

| Criterion | RED | GREEN / verification |
| --- | --- | --- |
| API-001 | OpenApiTest in each service: GET /v3/api-docs returned 404 | Adding the UI starter and service metadata produced 200 with the service identity and business paths. |
| API-002 | OpenApiTest in each service: optional correlation parameter missing | Required idempotency key, optional correlation ID, valid request example and success/error schemas pass. Swagger HTML, redirect and local Swagger config also pass. |
| DOC-001 | Mermaid CLI reproduced parse errors in order-success and inventory-unavailable at message semicolons | Replaced semicolons with commas. All three required diagrams render successfully and were visually inspected. Removed the extra SDD/TDD diagram. |

Focused RED and GREEN commands: `./mvnw -q -Dtest=OpenApiTest test`, from each
service directory. Both final independent `./mvnw -q clean verify` runs PASS:
Inventory **42 tests**, Order **63 tests**, zero failures/errors/skips. These
include all existing API/resilience tests plus three OpenAPI tests per service.

Exactly the original exercise's diagrams remain: services.mmd, order-success.mmd
and inventory-unavailable.mmd. Mermaid CLI 12.0.0 rendered each to a local PNG
using installed headless Chrome. Source files match the three Markdown previews;
local links and source conventions were checked. Renderer dependencies, images
and raw logs stay in ignored local validation storage, outside the application.

Implementation snapshot SHA-256: `bab82d473e859e717d08df1a9f9238bfa40ee03746d158c9e6514feb87093d26` (same manifest method as above).
Latest raw logs and renders are retained locally under `.local/swagger-evidence-*`.
The earlier real two-process E2E evidence remains historical; it was not rerun
for this Swagger/documentation update. Swagger's server endpoints/assets and
specification are covered by MockMvc; interactive browser clicks, container
startup and the live demo remain in the deferred testing phase.

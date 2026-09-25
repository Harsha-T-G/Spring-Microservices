# Chapter guide and implementation plan

## Scope and starting point

Source: `harsha_microservices_fundamentals_exercises.txt`, supplied by the user.
Initial planning began in an empty workspace. The user subsequently supplied
SpringMicroservices, containing a generated single-service starter. This foundation
captured specifications and development rules before coding. Application migration
and behavior slices are now implemented; see tasks and evidence for actual status.

The exercise is about correct behavior across a network boundary. RestClient is
the communication mechanism; ownership, duplicate handling, partial failures,
and tests are equally important learning goals.

The six chapter estimates total **7 hours 30 minutes**. That fits the stated
6–8 hour target only tightly; initial setup, learning, documentation, debugging,
and the demonstration may require additional time. Treat these as exercise
targets rather than a guaranteed completion estimate.

## Chapter-by-chapter understanding

| Chapter | What to understand | What we build or write | Completion check | Exercise estimate |
| --- | --- | --- | --- | --- |
| 1. Service boundaries | Separate services own separate business rules and data; the network introduces failures. | Boundaries, API contract, failure decisions, diagrams. | Explain why Order cannot change stock directly and how each Inventory outcome is handled. | 45 min |
| 2. Inventory | Thread-safe collections alone do not make a multi-step reservation atomic. Idempotency prevents duplicate effects. | Stock lookup, validation, reservation, atomic stock updates, reservation replay. | Public API tests prove correct stock changes, duplicate handling, and concurrency. | 90 min |
| 3. Order | One service coordinates with another through a contract and translates its outcomes. | Create/get/list orders, InventoryClient interface, RestClient adapter, order replay. | WireMock verifies the request and headers; repeated completed orders cause no new Inventory call. | 90 min |
| 4. Resilience | Timeout bounds waiting, retry handles selected transient failures, circuit breaker stops repeated attempts to an unhealthy dependency. | Configurable HTTP timeouts, one retry, circuit states, controlled errors. | WireMock proves selective retry, timeout, circuit opening, fast rejection, and recovery. | 90 min |
| 5. Observability | One correlation ID joins both services' logs; health is not proof of dependency connectivity. | Request filters, MDC cleanup, propagation, timing logs, health/info. | Header and error tests preserve the ID; logs connect a real order to its reservation. | 45 min |
| 6. Testing and failures | Tests observe public behavior and can substitute the remote service without replacing local business logic. | Independent suites, real two-service scenarios, evidence, demonstration. | All required scenarios have actual reproducible results. | 90 min |

## Implementation sequence

Work through small behaviors, not one large implementation per chapter. For
each behavior: write a failing test, implement enough to pass it, run the focused
test and relevant service suite, then continue. Capture at least three real
failing-to-passing cycles as they occur.

1. **Finish the specification (chapter 1).** Read the boundaries and API spec;
   review the proposed decisions, especially rejected-order responses and
   retrying an operation whose result is unknown.
2. **Scaffold independent applications.** Give each its own Maven build and
   wrapper, Java 21 configuration, port, packages, and local test setup. Verify
   that each can build without the other. Select compatible Boot 3.x, WireMock,
   and Resilience4j versions; do not copy current Boot 4-only configuration.
3. **Implement Inventory (chapter 2).** Stock lookup → validation → reservation
   → insufficient stock → replay → conflicting key → concurrent requests.
   Add consistent error handling as soon as the first error behavior is built.
4. **Implement Order against WireMock (chapter 3).** Reservation success → get
   and list → business rejection → unavailable dependency → completed replay
   → conflicting key → stable order identity across failed attempts.
5. **Add resilience (chapter 4).** Configure the HTTP transport timeout first,
   then selective retry, then the breaker with explicit composition order.
   Verify each behavior before combining the failure scenarios.
6. **Complete observability (chapter 5).** Add the correlation filter early in
   both applications so chapter 3 can test propagation; finish log fields,
   MDC cleanup tests, and Actuator configuration here.
7. **Run real services and finish evidence (chapter 6).** Add local Compose
   startup, run success/duplicate/rejection/unavailable/recovery scenarios,
   complete README and evidence, and rehearse the ten-minute demonstration.

Testing is continuous through steps 2–6. Chapter 6 consolidates and demonstrates
the work; it is not the first time tests are written.

## Planned code responsibilities

Each service has its own `controller`, `service`, `model`, `dto`, `exception`,
`config`, and in-memory storage responsibilities. Order additionally has a
`client` package. Use constructor injection throughout.

| Component | Responsibility |
| --- | --- |
| Controllers | Bind and validate HTTP input, delegate, choose response status/headers. |
| InventoryService | Apply stock rules and reservation idempotency atomically. |
| OrderService | Coordinate order identity, idempotency, reservation outcome, and storage. |
| InventoryClient | Order-owned interface for reserving stock using Order-owned types. |
| RestClientInventoryClient | Build the HTTP request; map remote status/body/transport failures into meaningful client outcomes. |
| Configuration | Build the HTTP client and resilience policies from configuration. |
| Exception advice | Produce the agreed error envelope without leaking internals. |
| Request filter | Manage correlation ID, MDC lifetime, and request completion logging. |

Avoid generic repository frameworks or abstractions beyond what these small
in-memory services need. A simple local store is sufficient.

## Acceptance and test matrix

| Area / public seam | Required scenarios |
| --- | --- |
| Inventory REST | Existing and missing SKU; case-insensitive matching; successful reservation; invalid quantity; missing/blank key; insufficient stock leaves stock unchanged. |
| Inventory replay and concurrency | Original reservation returned without decrement; changed order/SKU/quantity conflicts; concurrent distinct reservations cannot oversell; concurrent identical keys decrement once; same key across different SKUs conflicts. |
| Order REST | Confirmed order with Location; get existing/missing; list sorted by createdAt with empty array when empty; validation; clear insufficient/unknown-SKU rejection; stored rejected order. |
| Order dependency through WireMock | Correct order ID, quantity, key and correlation ID; completed replay causes no new request; changed payload conflicts; a retry after 503 retains the original order ID. |
| Resilience through WireMock | 400/404/409 are not retried; 500 is retried once; timeout and refused connection return 503; circuit opens; open circuit makes zero remote calls; recovery trial closes it. |
| Correlation and Actuator | Missing/invalid ID replaced; valid ID preserved; headers and error body agree; forwarding works; MDC does not leak; health/info expose service metadata only as configured. |
| Real two-service run | Successful reservation; duplicate; insufficient stock; unavailable Inventory within bounded time; recovery; matching IDs in both logs. |

Use MockMvc with real local service/storage components for full-service tests;
replace the remote HTTP boundary with WireMock. Use focused unit tests for
business behavior where useful. Do not test private methods or internal call
counts. Verify outbound request counts at WireMock only.

Use isolated state and fresh/reset resilience instances per test. Coordinate
concurrency with latches/barriers and bounded futures. Use bounded polling or
supported state controls for breaker timing; do not use `Thread.sleep`.
Timeout tests must execute the real configured HTTP client, not a mock timeout.

## Evidence and remaining documentation

Create `docs/test-evidence.md` when implementation starts. Record commands,
expected/observed failures, the change made, and the subsequent passing output.
Do not fabricate historical failing tests or copy placeholder success output.

The final README must include prerequisites, independent build/test/start
commands, configuration values, endpoint and failure tables, curl examples,
and limitations. Keep the three diagrams in service-boundaries.md linked from
the README. Extend microservices-notes.md with lessons from implementation.

The assignment also requests a PR with boundaries, executed checks, resilience
evidence, and limitations. Preparing/submitting that PR is later work, not an
action authorized by this planning request. Do not commit temporary review
reports; preserve these durable specs and required test evidence.

## Ten-minute demonstration outline

| Time | Demonstration |
| --- | --- |
| 0–1 min | Explain boundaries and independent application startup. |
| 1–3 min | Read stock, create an order, show reservation and reduced stock. |
| 3–4 min | Replay the same key and show unchanged stock. |
| 4–5 min | Request excessive stock and show controlled rejection. |
| 5–7 min | Simulate slow/unavailable Inventory, show retry and circuit opening. |
| 7–8 min | Restore Inventory and demonstrate recovery. |
| 8–9 min | Follow one correlation ID across both logs. |
| 9–10 min | Show both passing suites and explain in-memory limitations. |

Next implementation slice: Inventory stock lookup and unknown-SKU behavior,
preceded by one failing public API test.

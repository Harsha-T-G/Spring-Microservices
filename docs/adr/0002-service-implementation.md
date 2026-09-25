# 0002 — Implement the two-service exercise

Status: selected for implementation under the user's request to start coding,
2026-09-25. Supplements [0001](0001-exercise-scope-and-workflow.md).

## Decisions and reasons

- Pin Java 21, Boot 3.5.16, Resilience4j retry/circuitbreaker 2.3.0 and test-only
  WireMock standalone 3.13.2. Boot manages its own transitive versions. The
  [Spring release announcement](https://spring.io/blog/2026/06/25/spring-boot-3-5-16-available-now/)
  identifies this as the final OSS 3.5 release; it meets the assignment's 3.x
  constraint, while production would require a maintained baseline.
- Use two independent Maven projects. Keep the generated starter in feature
  branch history, replace the root application with service directories, and
  leave the initial main foundation unchanged. No shared model or parent build.
- Inventory uses a short synchronized store operation over private maps. The
  lock covers key lookup, fingerprint comparison, stock check/decrement and
  reservation insertion. This also coordinates cross-SKU key conflicts. Reads
  use the same lock. No controller locks or network calls under this lock.
- Order uses immutable request fingerprints and one bounded lock per key.
  Register a stable UUID/time before the HTTP call; retain it on technical
  failure. Completed orders live separately from pending attempt identity.
  Independent keys have no shared network lock. A persistent unique operation
  record and transactional stock update would replace these in production.
- Store known business rejections and respond 422 with orderId. Replay their
  original outcome using the current request's correlation ID. Technical
  uncertainty remains unfinalized, with 503 rather than a false REJECTED status.
  Unexpected dependency contracts map to 502; key conflicts retain 409.
- Use Spring RestClient with JDK HttpClient over HTTP/1.1 and explicit connect /
  response timeouts. Initial HTTP/2 upgrade negotiation against WireMock failed
  with EOF; forcing HTTP/1.1 passed the real transport contract without adding an
  HTTP dependency. Typed property validation enforces the exercise's limits.
- Compose resilience explicitly as CircuitBreaker(Retry(attempt)). One logical
  call contributes one breaker result. Retry only technical unavailability,
  at most twice total. Ignore expected business/key errors in breaker statistics.
  Record unexpected contracts as failures, but do not retry them. Tests cover
  CLOSED → OPEN → HALF_OPEN → CLOSED and failed trials reopening the circuit.
- Lombok immutable models validate constructor invariants. HTTP DTOs are records
  separately owned by each service. Constructor injection and conventional
  controller/service/client/store boundaries keep responsibilities visible.
- Use existing Boot logging and narrow Actuator exposure. No Security, CSRF,
  database, AOP starter, logging encoder or extra HTTP transport is needed.
- Container images copy the tested executable JAR and use a non-root Java 21
  runtime. This keeps Maven/testing outside the runtime image and means builds
  must precede Compose. Major-version image tags are suitable for this local
  exercise; deployment would pin scanned image digests.

## Limits and alternatives

A single application would avoid distributed uncertainty, but would not teach
this assignment's boundaries. Database transactions, unique idempotency records,
retention rules and reconciliation would be necessary with persistent data or
multiple replicas. In-memory maps grow without eviction and lose safety history
on restart. No cancellation or reservation-release workflow exists.

Automatic fallback success would misrepresent stock, so unavailable Inventory
always prevents confirmation. Order health checks only the local application.
The measured timeout bound applies to the controlled test; it is not an SLA for
DNS, machine overload, arbitrary payloads or an unbounded queue of client retries.

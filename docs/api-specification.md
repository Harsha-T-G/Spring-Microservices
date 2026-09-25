# API and behavior specification

Status: draft for implementation. **Required** means specified by the exercise.
**Proposed** means a concrete choice made here to fill an assignment gap.

## Common contract

Required: JSON APIs, constructor injection, validated requests, independent
service models and storage, configuration for URLs/timeouts, and correlation
IDs in response headers and errors.

Proposed input rules:

- `customerId`: nonblank, trimmed, case-sensitive.
- `sku`: nonblank, trimmed, normalized to uppercase using `Locale.ROOT` in both services. Inventory matching is case-insensitive as required.
- `quantity`: required integer greater than zero; reject null, fractions, and out-of-range values as 400 rather than silently coercing them.
- Reservation `orderId`: required valid UUID.
- `Idempotency-Key`: required on POST, case-sensitive, 1–128 characters from letters, digits, `.`, `_`, `:`, and `-`. Invalid/missing key returns 400.
- `X-Correlation-Id`: preserve a single value matching `[A-Za-z0-9._-]{1,64}`; otherwise generate a UUID. Do not reject the business request because this header is invalid.
- The idempotency key identifies an operation; the correlation ID identifies an HTTP request. A later client retry may have a different correlation ID without changing operation identity.

Proposed error envelope for **both** services (the six core fields are required
for Order; `code` and optional `orderId` are proposed additions):

```json
{
  "timestamp": "2026-09-24T12:00:00Z",
  "status": 503,
  "error": "Service Unavailable",
  "code": "INVENTORY_UNAVAILABLE",
  "message": "Inventory could not confirm the reservation. Retry with the same idempotency key.",
  "path": "/api/v1/orders",
  "correlationId": "demo-order-1"
}
```

Use stable codes to distinguish business errors. Never parse human-readable
messages to choose behavior. Never expose internal exception names, stack traces,
dependency URLs, or Resilience4j internals. Error correlation ID must match the
current response header, including when replaying a rejected order.

## Inventory API

| Endpoint | Required success | Required failures |
| --- | --- | --- |
| GET `/api/v1/inventory/{sku}` | 200 with `sku`, `availableQuantity` | 404 unknown SKU |
| POST `/api/v1/inventory/{sku}/reservations` | 201 with reservation details | 400 invalid quantity; 404 unknown SKU; 409 insufficient stock or conflicting key |

Development profile seed data: JAVA-BOOK = 20, KEYBOARD-01 = 10, MONITOR-24 = 5.
Tests initialize their own stock. Non-development profiles must not implicitly
load demonstration stock.

Reservation request:

```http
POST /api/v1/inventory/JAVA-BOOK/reservations
Content-Type: application/json
Idempotency-Key: customer-order-123
X-Correlation-Id: demo-order-1

{"orderId":"123e4567-e89b-42d3-a456-426614174000","quantity":2}
```

Reservation success body:

```json
{
  "reservationId": "223e4567-e89b-42d3-a456-426614174000",
  "orderId": "123e4567-e89b-42d3-a456-426614174000",
  "sku": "JAVA-BOOK",
  "quantity": 2,
  "status": "RESERVED"
}
```

Proposed error codes: `VALIDATION_ERROR`, `SKU_NOT_FOUND`, `INSUFFICIENT_STOCK`,
`IDEMPOTENCY_CONFLICT`. The latter two both use 409 but have different meanings.
Malformed JSON, missing orderId, or invalid UUID also return 400.

### Reservation atomicity and replay

Required: same key and same request returns the original successful reservation;
same key with different order/SKU/quantity returns 409; stock decrements once.

Proposed implementation: use one short lock inside the Inventory business/store
operation covering key lookup, fingerprint comparison, stock check, stock
decrement, and reservation insertion. Stock reads take the same lock for a
consistent view. Do not synchronize the controller or perform network calls
inside this lock. A single lock is sufficient for this small exercise and also
protects conflicting reuse across different SKUs.

The fingerprint is `(orderId, normalizedSku, quantity)`. Look for a stored key
before checking current stock, so a successful replay works even after stock
is exhausted. Replays return 201 and the original business response; the
correlation header belongs to the current request. Cache successful reservations
only; failures do not decrement stock or create reservations. These replay-status
and failure-caching policies are proposed, not specified by the exercise.

ConcurrentHashMap alone is insufficient: a separate read/check/write sequence
can race. For example, two requests can both read stock = 1 before each reserves
one unit. The complete reservation operation must be atomic.

## Order API

| Endpoint | Contract |
| --- | --- |
| POST `/api/v1/orders` | Required: 201 CONFIRMED with Location and both IDs; 409 conflicting key; controlled 503 for unavailable Inventory. Proposed: 422 for a business-rejected order. |
| GET `/api/v1/orders/{id}` | Required: 200 existing order, 404 missing. Proposed: malformed UUID is 400. |
| GET `/api/v1/orders` | Required: 200 array, ascending createdAt, empty array when no orders. Proposed: ID tie-breaker for equal timestamps. |

Create request:

```http
POST /api/v1/orders
Content-Type: application/json
Idempotency-Key: customer-order-123
X-Correlation-Id: demo-order-1

{"customerId":"CUST-1001","sku":"JAVA-BOOK","quantity":2}
```

Confirmed response example:

```http
HTTP/1.1 201 Created
Location: /api/v1/orders/123e4567-e89b-42d3-a456-426614174000
X-Correlation-Id: demo-order-1
Content-Type: application/json

{
  "id": "123e4567-e89b-42d3-a456-426614174000",
  "customerId": "CUST-1001",
  "sku": "JAVA-BOOK",
  "quantity": 2,
  "status": "CONFIRMED",
  "reservationId": "223e4567-e89b-42d3-a456-426614174000",
  "rejectionReason": null,
  "createdAt": "2026-09-24T12:00:00Z"
}
```

### Outcome translation

The assignment does not specify the HTTP status/body for business rejections.
Proposed: store a REJECTED order for insufficient stock or unknown SKU, return
422 using the common error envelope plus its `orderId`, and make the full order
available through GET. The stored order has a null reservationId and a stable
rejectionReason matching the business error code.

| Inventory result | Client outcome / Order response | Retry? |
| --- | --- | --- |
| Valid 201 RESERVED | CONFIRMED; 201 + Location | No |
| 409 `INSUFFICIENT_STOCK` | Store REJECTED; 422 `INSUFFICIENT_STOCK` + orderId | No |
| 404 `SKU_NOT_FOUND` | Store REJECTED; 422 `SKU_NOT_FOUND` + orderId | No |
| 409 `IDEMPOTENCY_CONFLICT` | 409 `IDEMPOTENCY_CONFLICT`; do not falsely label stock unavailable | No |
| Unexpected 400 or other unexpected 4xx | Proposed 502 `INVENTORY_CONTRACT_ERROR`; valid Order input should not produce this | No |
| 5xx, connection failure, or timeout | 503 `INVENTORY_UNAVAILABLE` after exhaustion | Once |
| Invalid/empty success body, mismatched IDs/SKU/quantity, unrecognized 404/409 body | Proposed 502 `INVENTORY_CONTRACT_ERROR` | No |
| Circuit open | 503 `INVENTORY_UNAVAILABLE` | No |

Unexpected downstream 4xx or malformed responses indicate an integration problem,
not necessarily a bad customer request. Do not store these or 503 outcomes as
business-rejected orders.

### Order identity, replay, and uncertain outcomes

Required: repeat a completed order without calling Inventory again; changed
customer/SKU/quantity under the same key returns 409.

Proposed local coordination:

1. Validate input and compute `(trimmedCustomerId, normalizedSku, quantity)`.
2. Atomically register the key, fingerprint, original order UUID, and createdAt
   before making the first Inventory call. Reuse that UUID for every attempt.
3. For a completed operation, return the original order outcome: 201 and original
   Location for confirmed; 422 and original orderId/reason for rejected. GET
   returns the stored order. Do not cache old correlation headers or error timestamps.
4. For concurrent matching requests, allow one active reservation operation per
   key. A second request can wait for the same result with a bounded wait, then
   return 503 `ORDER_IN_PROGRESS` if still unresolved. It must not start a parallel
   operation. Different keys can proceed independently.
5. After a technical failure, retain the original fingerprint, UUID, and createdAt
   so a later matching request can safely try again. Return 409 for changed data.
   Internal attempt state is separate from the public OrderStatus enum. GET/list
   expose only finalized CONFIRMED or REJECTED orders.

Do not hold a global Order lock while calling Inventory. Do not generate a new
UUID on every retry. A timed-out Inventory request may already have reserved
stock; a new UUID with the old key would then conflict.

A duplicate REJECTED order remains rejected even if conditions later change;
creating a genuinely new order uses a new key. A timeout does not establish
rejection. Retained identities and reservation records provide replay safety
only while both in-memory stores survive. No cancellation or stock-release
endpoint is added for this exercise.

## RestClient and resilience

Required: `InventoryClient` interface and `RestClientInventoryClient`
implementation. OrderService calls the interface with the order ID, SKU,
quantity, key, and correlation ID. HTTP URLs, serialization, response mapping,
and transport configuration stay outside OrderService.

Use one configured RestClient. It supports request factories for the underlying
HTTP transport and status handlers for response mapping. Configure transport
timeouts explicitly; a retry policy does not impose them.
[Spring Framework 6.2 REST client documentation](https://docs.spring.io/spring-framework/reference/6.2/integration/rest-clients.html).

Proposed configuration defaults (names represent our planned configuration,
not a claim that all are built-in Spring properties):

| Setting | Default | Requirement / rationale |
| --- | --- | --- |
| Inventory base URL | `http://localhost:8081` | External configuration; Compose overrides hostname to `inventory-service`. |
| Connection timeout | 500 ms | Exercise maximum; validate configured upper bound. |
| Response timeout | 1000 ms | Exercise maximum; validate configured upper bound. |
| Retry maxAttempts | 2 | Initial attempt + one additional attempt. |
| Retry wait | 100 ms | Small configurable delay for the demo. |
| Breaker window type/size | COUNT_BASED / 4 | Small demonstration window. |
| Breaker minimum calls | 4 | Avoid opening on the very first failure. |
| Breaker failure threshold | 50% | Open when threshold is reached in the measured window. |
| Breaker OPEN wait | 5 s | Next eligible call can become a recovery trial. |
| HALF_OPEN permitted operations | 1 | One successful trial closes; failed trial reopens. |

`maxAttempts` includes the first attempt; configure a predicate for connection
failures, timeouts, and HTTP 5xx only. Log retry number and correlation ID without
the request body. [Resilience4j Retry](https://resilience4j.readme.io/docs/retry).

Proposed explicit nesting: **CircuitBreaker(Retry(single HTTP attempt))**.
The breaker records one final outcome per logical reservation operation. Its
half-open trial may make up to two HTTP attempts. Ignore expected business
exceptions for breaker statistics; record dependency/contract failures.
Do not rely on implicit annotation ordering. CLOSED permits and records calls;
OPEN rejects them; HALF_OPEN evaluates a limited trial after the wait.
[Resilience4j CircuitBreaker](https://resilience4j.readme.io/docs/circuitbreaker).

With these defaults, two connection-plus-response waits and retry delay give
an approximate 3.1-second budget before local overhead. This is not a hard
whole-request guarantee: confirm actual transport semantics, bound any pool
acquisition wait, and measure elapsed time. Proposed controlled-test limit:
under 4 seconds for an isolated unavailable/slow dependency with these defaults.
Do not claim DNS, scheduling, or response-body handling is bounded without
checking the selected transport. There is no success fallback that fabricates
stock or marks an unavailable reservation CONFIRMED.

## Correlation, logging, and health

In each service, a request filter validates/generates the correlation ID, sets
the response header, and places it in MDC. Clear MDC in `finally`, including
error paths. Forward the current ID from Order on both HTTP attempts.

Log service name, correlation ID, method/path, response status, elapsed time,
and order/reservation ID when available. Inventory logs the ID received from
Order and returns it. Do not log full request bodies, secrets, or expected-4xx
stack traces.

Expose only Actuator `health` and `info`, with service name/version in info.
Proposed: local health only, no Inventory connectivity indicator. Document
that Order health UP does not imply Inventory is reachable or orders can be
confirmed. Do not label local health as a dependency readiness check.

## Decisions to revisit before the corresponding implementation

The exercise fixes Java 21, Boot 3.x, RestClient, service boundaries, endpoint
paths, and resilience limits. It leaves the following choices to us:

- 422 plus stored REJECTED order versus another explicit rejection contract.
- 201 on successful replay, and replaying rejected outcomes consistently.
- 502 for unexpected dependency contracts and stable machine-readable codes.
- Header validity rules and normalization rules.
- Short Inventory lock and bounded per-key Order coordination.
- Stable unresolved order identity and limitations after restarts.
- Breaker nesting and demonstration thresholds.
- Exact compatible dependency versions and HTTP transport timeout semantics.

These are reviewable defaults, not additional features mandated by the source.

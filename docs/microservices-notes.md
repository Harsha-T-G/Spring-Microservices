# Microservices fundamentals notes

These learning notes describe the implemented two-service exercise. Use them
with the [chapter guide](implementation-plan.md), [implementation decisions](adr/0002-service-implementation.md),
and [verification evidence](test-evidence.md) for the design rationale and observed results.

## What makes these separate services?

Each has its own application process, Maven build, port, business responsibilities,
and storage. They communicate using a published HTTP contract. Putting two
controllers in the same application would not create this separation.

Order and Inventory are separated to practice ownership and remote communication.
For this small domain, a single application would be simpler to build and run.
The exercise deliberately introduces the trade-offs of a network boundary.

## Who owns the data and rules?

Order owns orders and customer-facing order status. Inventory owns stock and
reservation rules. Order can request a reservation but cannot update stock.
Inventory records the supplied order ID without managing the order lifecycle.

Directly querying another service's tables or sharing internal classes allows
one application to depend on implementation details and bypass ownership. The
shared PostgreSQL server is split into service-owned schemas; applications
still communicate through HTTP. Matching JSON contracts do not require shared
Java DTO classes.

## What coupling still exists?

Order depends on Inventory's API path, request and response shape, business error
codes, latency, and availability. Independent deployment does not mean zero
coupling. WireMock verifies Order's assumptions; real two-service tests verify
that the actual provider matches those assumptions.

If Inventory breaks its contract, Order can fail even if Order itself did not
change. Preserve backward compatibility or coordinate a versioned API change.
Independent DTOs do not automatically solve API compatibility.

## Why RestClient, and where does it belong?

The exercise requires synchronous REST using RestClient. Spring describes it as
a synchronous client with a fluent request API; Order waits for the reservation
result. The client adapter owns URL construction, headers, body conversion, and
remote error mapping. The interface gives OrderService a business-focused
boundary without exposing HTTP details.
[Spring REST clients](https://docs.spring.io/spring-framework/reference/6.2/integration/rest-clients.html).

RestTemplate is excluded by the assignment. We do not need reactive programming
or an asynchronous callback to satisfy this flow.

## Why is idempotency necessary?

Imagine Inventory reserves two books but the response is lost. Retrying an
ordinary POST might reserve two more. Remembering the key and original result
lets Inventory return the first reservation without repeating the effect.

Order also remembers completed results so client retries do not call Inventory
again. A key must be tied to the original request values; reusing it for a
different order is a conflict. After a timeout, preserving the original order
ID matters just as much as preserving the key.

## What prevents overselling?

Checking availability, reducing stock, and saving the idempotent reservation
must happen atomically. A thread-safe map protects individual map operations,
not a sequence of business operations. The short Inventory store lock makes
that sequence indivisible within one application process.

Production would use durable transactional storage with concurrency control
and unique idempotency keys. A JVM lock cannot protect other replicas.

## Timeout, retry, circuit breaker, and fallback

A timeout bounds a particular wait. A retry makes another attempt after a
selected failure. A circuit breaker temporarily stops attempts when failures
indicate an unhealthy dependency. A fallback supplies an alternative result;
here the controlled 503 is a failure response, not a fabricated reservation.

Only connection failures, timeouts, and 5xx qualify for one extra attempt.
Validation errors, unknown SKU, and insufficient stock will not become correct
because they were immediately repeated. Without a timeout, the first attempt
could wait indefinitely and never reach the retry.

A retry storm is many callers repeatedly retrying a failing service, increasing
load during recovery. Bounded attempts and circuit breaking reduce that load;
production designs may also use backoff and jitter.

## What happens when only one service completes?

Inventory can reserve stock while Order loses the reply. A network timeout does
not prove rollback. This is why the implementation keeps unresolved operation
identity and allows a safe retry instead of recording a business rejection.

One ordinary database transaction cannot atomically cover both independent
stores and the HTTP exchange. Distributed coordination or compensation would
require additional protocols and design, outside this exercise.

## Eventual consistency and messaging

Eventual consistency means that related views may temporarily disagree but
converge when outstanding changes are successfully processed. It needs an actual
recovery mechanism; simply using multiple services does not guarantee convergence.

This exercise is synchronous because the Order request waits for Inventory's
HTTP response. It can still experience partial completion. There is no background
reconciliation worker, so abandoned attempts do not automatically converge.

Messaging could be useful when the customer can accept a pending order and
reservation can happen later. Kafka or RabbitMQ would introduce events,
consumers, delivery/ordering concerns, eventual outcomes, and still require
idempotency. Messaging would not automatically provide exactly-once business
effects. Do not add it to this assignment.

## How do correlation IDs and tests help?

The same correlation ID links a customer's Order request to its Inventory call
and logs. MDC makes it available to log formatting; it must be cleared because
request threads are reused. Distributed tracing could add parent/child spans,
timing, and dependency views beyond a shared log identifier.

MockMvc tests public HTTP behavior using real local business code. WireMock
acts as the remote Inventory endpoint to test outbound contracts and controlled
failures without starting Inventory. Real two-service scenarios verify the
integration that separate suites alone cannot establish.

## What would production need?

Durable data and idempotency, recovery/reconciliation, retention limits,
multi-instance concurrency control, authentication/authorization, secure
configuration, metrics/tracing, deployment operations, and contract evolution.
Choose additional infrastructure based on requirements rather than assuming
every microservice needs a gateway, discovery server, or message broker.

PostgreSQL preserves orders, reservations, stock and replay history across
application restarts. One database does not make the two service processes and HTTP exchange one
atomic transaction. Independent failures can still require reconciliation beyond
an additional retry.

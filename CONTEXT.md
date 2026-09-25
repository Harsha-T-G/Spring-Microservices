# Domain vocabulary and starting point

- **Order:** customer request for a SKU and quantity, owned by Order Service.
- **Reservation:** Inventory's recorded stock allocation to an order ID.
- **Confirmed:** Inventory returned a valid successful reservation.
- **Rejected:** known business rejection, such as insufficient stock.
- **Unknown outcome:** a timeout/transport failure; Inventory may have completed.
  It does not prove rejection or rollback.
- **Idempotency key:** stable operation identity, bound to normalized request data.
- **Correlation ID:** tracing value for an HTTP request; it may change on a later
  client retry and is not a substitute for the idempotency key.
- **Logical reservation operation:** one breaker observation wrapping
  at most two HTTP attempts.
- **Capability:** documentation/delivery grouping, not an additional microservice.

Order runs on 8080; Inventory runs on 8081. Order calls Inventory synchronously
with RestClient. Inventory never calls Order back. Each owns its own data/types.

Initial inspection found a single generated Boot 4.1.1/Java 17 starter with
Lombok and devtools, and no Git metadata. This is input to preserve, not evidence
of compliance with the exercise's Java 21/Boot 3.x/two-service requirement.

Implementation now exists on feat/independent-services: two Java 21 / Boot
3.5.16 applications, atomic Inventory reservations, Order RestClient integration,
replay identity, bounded retry/circuit breaker and correlation handling. Initial
main remains the foundation. API defaults were selected as implementation
choices under the request to start coding; they are not extra assignment mandates.

See docs/test-evidence.md for verified behavior and remaining delivery limits.
The root generated starter is preserved in feature-branch history; build from
each service directory. No remote/PR is configured or published yet.

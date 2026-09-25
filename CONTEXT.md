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
- **Logical reservation operation:** one proposed breaker observation wrapping
  at most two HTTP attempts.
- **Capability:** documentation/delivery grouping, not an additional microservice.

Order runs on 8080; Inventory runs on 8081. Order calls Inventory synchronously
with RestClient. Inventory never calls Order back. Each owns its own data/types.

Initial inspection found a single generated Boot 4.1.1/Java 17 starter with
Lombok and devtools, and no Git metadata. This is input to preserve, not evidence
of compliance with the exercise's Java 21/Boot 3.x/two-service requirement.

Current scope is the documentation and agentic foundation. The first application
slice is the independent-service scaffold, followed by Inventory stock lookup
using TDD. Choices marked proposed in the API contract remain proposals.

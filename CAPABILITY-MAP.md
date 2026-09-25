# Capability map

Only Order and Inventory become application services. IDs below organize
acceptance criteria, tests and PR evidence; they do not create Java packages.

| Capability | IDs | Responsibility | Depends on |
| --- | --- | --- | --- |
| foundation | FND | Independent toolchain, conventions and local skills | — |
| inventory | INV | Lookup and atomic idempotent reservations | foundation |
| order | ORD | Order lifecycle and RestClient boundary | foundation, Inventory contract |
| resilience | RES | Timeouts, selective retry and circuit recovery | order |
| observability | OBS | Correlation, logs and health/info | foundation |
| delivery | DEL | Two-service scenarios, evidence and demonstration | all application capabilities |

Criteria live in [docs/spec.md](docs/spec.md); detailed payload and behavior
contracts live in [docs/api-specification.md](docs/api-specification.md).
Build foundation → Inventory → Order → resilience → integrated delivery.
Introduce correlation infrastructure with the first HTTP slices and finish
observability checks before delivery. WireMock lets Order develop against the
agreed Inventory contract without requiring a live Inventory process.

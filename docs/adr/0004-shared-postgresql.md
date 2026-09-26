# ADR 0004: One PostgreSQL database with service schemas

Status: accepted by the user's explicit request for one database. This changes
the physical topology selected in ADR 0003; PostgreSQL and Flyway remain.

One PostgreSQL container hosts the `microservices` database. Inventory stores
`product_stock`, `reservations`, and its Flyway history under schema `inventory`.
Order stores `orders` and its Flyway history under schema `orders`. Both services
use one `DB_URL`, `DB_USER`, and `DB_PASSWORD` pair from the local `.env`, but
independent JVMs still maintain separate JDBC connection pools. Each service's
Flyway configuration creates and migrates only its own schema; its JDBC pool
sets that schema for unqualified queries. The REST boundary remains the only
way Order requests an Inventory reservation.

This is simpler to run locally: one container, one host port, one named volume,
and one credential pair. It reduces database-level isolation: with the shared
demonstration role, either service could technically query the other's schema.
That access remains forbidden by the service contract and absent from code.
A production design requiring enforced ownership would use separate roles with
schema grants, or return to separate databases. Sharing the physical database
does not make HTTP coordination one transaction, so uncertain outcomes still
require stable idempotency keys and retry/reconciliation policy.

Existing `inventory-data` and `order-data` volumes from ADR 0003 are preserved
when the new configuration is applied; they are not automatically migrated into
the new `postgres-data` volume. A fresh local demo starts with Inventory's dev
seed. Preserve or explicitly migrate existing data before retiring old volumes.

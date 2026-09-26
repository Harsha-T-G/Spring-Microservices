# ADR 0003: Separate PostgreSQL databases and Flyway migrations

Status: accepted by the user's explicit storage request, 2026-09-26. This
supersedes the in-memory choice in ADR 0002. The exercise says each service
*may* use in-memory data; it does not require that choice.

Inventory owns product stock and reservation history in its PostgreSQL database.
Order owns attempted and completed orders in another PostgreSQL database. Neither
service connects to the other's database. Both use Spring JDBC for direct,
small SQL stores and Flyway for versioned schema. Inventory's `dev` profile adds
three demonstration SKUs through a separate Flyway migration; the default
profile starts with an empty catalog. Seeds run once per database, so restart
never resets quantities or idempotency history.

Inventory reserves inside one local transaction. A transaction-scoped advisory
lock serializes the idempotency key, including cross-SKU use; an atomic guarded
UPDATE prevents negative stock across concurrent requests. The reservation row
and stock update commit or roll back together. Order persists its request/key
and UUID before the HTTP call, then stores the final outcome. Technical failures
leave the attempt pending so the same key can retry with the same UUID. Existing
process-local locks bound concurrent duplicate Order requests; multiple Order
replicas are not supported without additional distributed coordination.

Compose runs two named-volume PostgreSQL instances with readiness checks.
Dockerfiles now build the applications in a Maven stage, so `docker compose up
--build` does not require prebuilt `target` JARs. Runtime images still use the
Java 21 JRE and non-root user. Testcontainers supplies isolated PostgreSQL for
service tests. This adds the minimum persistence dependencies: Spring JDBC,
PostgreSQL driver, Flyway core/PostgreSQL support, and test-scope Testcontainers.

Separate local transactions cannot atomically cover the Inventory HTTP exchange.
A timeout can still leave uncertain Order/Inventory outcomes; the durable key
and stable order ID make a retry safe, but reconciliation/compensation, retention,
backups, credential management, and multi-replica coordination remain future
production concerns. `docker compose down -v` intentionally removes demo data.

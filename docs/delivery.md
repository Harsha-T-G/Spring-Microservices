# Review and publication

Implementation branch: `feat/independent-services`. Initial `main` remains the
foundation. [PR #1](https://github.com/Harsha-T-G/Spring-Microservices/pull/1) is open
against main. Swagger, diagram and PostgreSQL persistence updates use the same branch.
The following is durable delivery documentation, not a generated review report.
Use it with the repository's [PR template](../.github/pull_request_template.md)
when an intended remote is available. Do not upload ignored logs or local backups.

Suggested title: **feat: implement persistent Order and Inventory services**

Suggested description:

> Implements the Mini Order and Inventory exercise as independent Java 21 /
> Spring Boot 3 applications. Order calls Inventory through RestClient;
> Inventory owns atomic stock reservations. Replays preserve the original
> order/reservation, including stable order identity after uncertain failures.
>
> Selective retry and an explicit circuit breaker return controlled 503s without
> fabricating successful orders. Correlation IDs connect both services' logs.
> Lombok models, API validation, safe error envelopes and local health/info follow
> the project conventions, with separate PostgreSQL databases, Flyway migrations, and no added
> security or logging stack.
>
> Validation: see the latest PostgreSQL entry in test-evidence.md for the
> current clean verify counts and real-process restart checks.
> See [test evidence](test-evidence.md) for authentic RED/GREEN cycles, exact
> commands, timing and retry/circuit call counts.
>
> Contracts: [specification](spec.md), [API](api-specification.md),
> [boundaries](service-boundaries.md), [persistence ADR](adr/0003-postgresql-persistence.md).
> Diagrams: [services](diagrams/services.mmd), [success](diagrams/order-success.mmd),
> [unavailable](diagrams/inventory-unavailable.mmd).
>
> Limits: Order request locks are process-local, so multiple Order replicas
> require coordination. There is no cross-service transaction or stock-release
> workflow. The ten-minute demo is prepared, not yet presented. Boot 3.5.16
> is the exercise baseline, not a maintained production support claim.

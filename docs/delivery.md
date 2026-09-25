# Review and publication

Implementation branch: `feat/independent-services`. Initial `main` remains the
foundation. No remote is configured, so there is no published PR or PR URL.
The following is durable delivery documentation, not a generated review report.
Use it with the repository's [PR template](../.github/pull_request_template.md)
when an intended remote is available. Do not upload ignored logs or local backups.

Suggested title: **feat: implement independent Order and Inventory services**

Suggested description:

> Implements the Mini Order and Inventory exercise as independent Java 21 /
> Spring Boot 3 applications. Order calls Inventory through RestClient;
> Inventory owns atomic stock reservations. Replays preserve the original
> order/reservation, including stable order identity after uncertain failures.
>
> Selective retry and an explicit circuit breaker return controlled 503s without
> fabricating successful orders. Correlation IDs connect both services' logs.
> Lombok models, API validation, safe error envelopes and local health/info follow
> the project conventions, with no added security/database/logging stack.
>
> Validation: Inventory clean verify 39 tests; Order clean verify 60 tests;
> all pass with zero failures, errors or skips on native JDK 21. Real-process
> success, replay, insufficient stock, shutdown, open circuit and recovery pass.
> See [test evidence](test-evidence.md) for authentic RED/GREEN cycles, exact
> commands, timing and retry/circuit call counts.
>
> Contracts: [specification](spec.md), [API](api-specification.md),
> [boundaries](service-boundaries.md), [implementation ADR](adr/0002-service-implementation.md).
> Diagrams: [services](diagrams/services.mmd), [success](diagrams/order-success.mmd),
> [unavailable](diagrams/inventory-unavailable.mmd).
>
> Limits: in-memory history is lost on restart and is not safe across replicas;
> maps have no eviction; no persistence or stock-release workflow. Native
> end-to-end verification passed; container startup remains NOT RUN (Compose
> configuration validation passed). The ten-minute demo is prepared, not yet
> presented. Boot 3.5.16 is the exercise baseline, not a maintained production
> support claim.

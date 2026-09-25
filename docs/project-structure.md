# Structure and starter transition

Use conventional layer-based Java packages in each independent application.
Do not copy the earlier database/security project structure wholesale.

```text
SpringMicroservices/
  AGENTS.md / CONTEXT.md / CAPABILITY-MAP.md / README.md
  .agents/skills/                 local SDD and TDD
  .guidelines/                    Java, Spring Boot and tests
  .github/pull_request_template.md
  .mvn/ + mvnw + mvnw.cmd          foundation toolchain
  docs/
    spec.md / api-specification.md / service-boundaries.md
    implementation-plan.md / project-structure.md / microservices-notes.md
    plans/tasks.md
    adr/
    diagrams/*.mmd
    test-evidence.md
    skill-provenance.md / skill-sources/
    templates/
  inventory-service/              planned on feature branch
    pom.xml / mvnw / mvnw.cmd / .mvn/
    src/main/java/org/example/inventory/
      InventoryApplication.java
      config/ controller/ service/ model/ dto/ store/ exception/
      observability/
    src/main/resources/application.yml
    src/test/java/org/example/inventory/
      controller/ service/ model/ observability/ support/
    src/test/resources/
  order-service/                  same independent Maven layout, planned
    src/main/java/org/example/order/
      OrderApplication.java
      config/ controller/ service/ model/ dto/ store/ exception/
      client/                     InventoryClient + RestClientInventoryClient
      observability/
  compose.yml                     planned two-service startup
  .local/                         ignored logs and disposable reviews
```

`org.example.inventory` and `org.example.order` are proposed namespaces based
on the starter. Create packages only when needed, not empty placeholders.
Models use Lombok; DTOs are separate. No JPA entity/repository/security packages,
generic store interfaces or ServiceImpl pairs are required. Add mappers only
when conversion is repeated or nontrivial.

Initial main contains foundation files only. The supplied root pom/src remain
preserved for `feat/independent-services`; they are excluded from the initial
main commit. Convert their Java 17/Boot 4.1.1 single-service setup into two
Java 21/Boot 3.x applications on that branch. This is a planned change, not an
already completed migration.

Each service must build with its own wrapper and pom without the other's build.
A custom root parent/aggregator is unnecessary. A root wrapper may remain a
convenience with `-f`, not the only build path. Later reviewed feature merges
may advance main normally.

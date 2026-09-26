# Structure and starter transition

Use conventional layer-based Java packages in each independent application.
Use small local JDBC stores and Flyway; do not copy the earlier JPA/security
project structure wholesale.

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
  inventory-service/              implemented on feature branch
    pom.xml / mvnw / mvnw.cmd / .mvn/
    src/main/java/org/example/inventory/
      InventoryApplication.java
      config/ controller/ service/ model/ dto/ store/ exception/
      observability/
    src/main/resources/application.yml / application-dev.yml
    src/main/resources/db/migration/ / db/dev/
    src/test/java/org/example/inventory/
      config/ controller/ model/ observability/
  order-service/                  same independent Maven layout
    src/main/java/org/example/order/
      OrderApplication.java
      config/ controller/ service/ model/ dto/ store/ exception/
      client/                     InventoryClient + RestClientInventoryClient
      observability/
  scripts/verify-e2e.py            real-process verification
  compose.yml                     two services and one PostgreSQL database
  .local/                         ignored logs and disposable reviews
```

`org.example.inventory` and `org.example.order` are namespaces based
on the starter. Create packages only when needed, not empty placeholders.
Models use Lombok; DTOs are separate. No JPA entity/repository/security packages,
generic store interfaces or ServiceImpl pairs are required. Add mappers only
when conversion is repeated or nontrivial.

Initial main contains foundation files only. The supplied root pom/src are
preserved in feature-branch commit 7b63c19. Their Java 17/Boot 4.1.1 single-service
setup is replaced by two Java 21/Boot 3.5.16 applications on feat/independent-services.

Each service must build with its own wrapper and pom without the other's build.
A custom root parent/aggregator is unnecessary. A root wrapper may remain a
convenience with `-f`, not the only build path. Later reviewed feature merges
may advance main normally.

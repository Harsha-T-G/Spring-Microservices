# Mini Order and Inventory System

Agentic development foundation for the Spring Boot microservices exercise.
Application implementation has not started; the supplied starter is preserved
for the first implementation branch.

## Start here

1. [Agent instructions](AGENTS.md), [domain context](CONTEXT.md), and [capability map](CAPABILITY-MAP.md).
2. [Acceptance criteria](docs/spec.md) and [chapter-by-chapter plan](docs/implementation-plan.md).
3. [Service boundaries and diagrams](docs/service-boundaries.md) and [API contract](docs/api-specification.md).
4. [Project structure](docs/project-structure.md), [Java conventions](.guidelines/java.md), [Spring conventions](.guidelines/spring-boot.md), and [testing conventions](.guidelines/testing.md).
5. [Implementation slices](docs/plans/tasks.md), [evidence status](docs/test-evidence.md), and [learning notes](docs/microservices-notes.md).

## Local SDD and TDD

Use the project-owned [SDD skill](.agents/skills/spec-driven-development/SKILL.md)
and [TDD skill](.agents/skills/test-driven-development/SKILL.md). They contain the
workflow locally, not a redirect to an upstream skill. Pinned source snapshots,
licenses, hashes and adaptations are recorded in [provenance](docs/skill-provenance.md).
Updates are deliberate local changes; upstream changes cannot silently apply.

Flow: spec and acceptance criteria → behavior slice → failing test → minimum
code → passing test → refactor → relevant suite → recorded PR evidence.
See [workflow diagram](docs/diagrams/sdd-tdd-workflow.mmd).

## Branch and technology baseline

Initial `main` contains toolchain, specs, diagrams and agent guidance. Application
work goes on `feat/<reason>`, starting with `feat/independent-services`.
Publishing requires the intended remote; no remote or PR URL is invented.

The assignment requires Java 21, Boot 3.x, independent Maven services on 8080 and
8081, RestClient, validation, Actuator, JUnit 5/MockMvc/WireMock, Resilience4j,
SLF4J/Logback, and Lombok domain models. The supplied single-service starter uses
Java 17/Boot 4.1.1 and needs conversion on the feature branch.

In-memory storage is for learning. No auth/CSRF/security stack, database, shared
Java module, gateway, discovery, messaging or frontend is planned. See the
[scope/dependency ADR](docs/adr/0001-exercise-scope-and-workflow.md).

Service build/start commands, curl examples and operational evidence will be
added and executed as the services exist. Current planned commands are explicitly
marked in the specs/tasks; this README does not claim the starter meets them.
Use the [PR template](.github/pull_request_template.md) and commit curated
evidence, while keeping disposable reviews and raw validation output local.

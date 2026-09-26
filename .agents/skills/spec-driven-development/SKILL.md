---
name: spec-driven-development
description: Maintain SpringMicroservices specifications, acceptance criteria, diagrams and implementation slices before behavior changes. Use for feature planning, contract changes and assignment ambiguity.
---

# Project-owned spec-driven development

Locally maintained adaptation of Addy Osmani's SDD skill. It executes entirely
from this repository. Source revision, license and changes are recorded in
[provenance](../../../docs/skill-provenance.md). Do not fetch upstream at runtime.

## Establish the contract

Read root AGENTS.md, CONTEXT.md, CAPABILITY-MAP.md, the affected criteria in
docs/spec.md and the relevant API contract. Distinguish assignment requirements,
explicit user choices, proposals and observed implementation. Presence in Git
does not establish approval of a proposed contract.

Specifications define observable behavior before code. Preserve document paths
and domain vocabulary. Small fixes need a criterion/amendment, not a new framework
or a duplicate spec. Keep unrelated capabilities out of the active context.

## Specify → plan → tasks → implementation

1. Specify objective, actor, normal/error flow, validation, data ownership,
   concurrency, idempotency, non-goals and testable criteria with stable IDs.
   Keep detailed HTTP contracts in docs/api-specification.md and link them.
2. Plan the smallest design satisfying those criteria. Explain boundaries,
   build order, configuration, dependencies, risks, test seams and commands.
   Apply the local Java/Spring/test guidelines.
3. Record sequential behavior slices in docs/plans/tasks.md. Each identifies
   criteria, affected responsibilities/files, failing scenario and verification.
4. Implement through the local test-driven-development skill, on feat/reason
   before application edits. Update task/evidence with actual results.

Human review resolves meaningful ambiguity; it is not a repeated approval ritual.
User authorization and accepted decisions persist. Proceed within scope. Ask
only when a missing answer materially changes behavior/scope or an important
action lacks authorization. This skill does not authorize publication or deployment.

## Artifacts and change control

- docs/spec.md indexes criteria; CAPABILITY-MAP.md defines ownership.
- docs/diagrams/*.mmd are editable sources; embedded previews are derived copies.
- docs/adr records material trade-offs with alternatives, not every small method.
- Update changed decisions/contracts before implementing them. Clearly mark
  superseded decisions instead of silently rewriting their history.
- Link criterion → test → result in evidence and PRs. A created class/file is
  not a delivered capability.
- Keep required docs and curated evidence; raw logs/disposable reviews stay local.

## Project constraints

Two independent Java 21/Boot 3.x applications, synchronous RestClient, separate
PostgreSQL data ownership, Flyway migrations, Lombok domain models and bounded
resilience. Do not inherit auth/CSRF/extra logging from the previous POC. Use available framework
support before adding dependencies, preserving atomic stock, stable operation
identity and meaningful tests.

Before handoff, check testable criteria, visible proposals, local links, diagrams
against contracts, appropriate verification results and the next concrete slice.

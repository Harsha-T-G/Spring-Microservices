# ADR 0001: Small independent services and local development rules

Status: accepted for workflow/user-mandated scope; API proposals remain marked
in their specification. Recorded 2026-09-25.

## Context

The exercise teaches synchronous communication and partial failure, not a full
production platform. The user requires project-owned SDD/TDD, conventional Java
layout, Lombok models, comment-free production code, and evidence-led PRs.
The starter is one Java 17/Boot 4.1.1 application, unlike the assignment baseline.

## Decisions and alternatives

| Decision | Reason | Alternative not selected |
| --- | --- | --- |
| Two standalone Maven applications | Independent builds/data ownership are explicit exercise goals. | Shared domain/DTO module or one deployed application. |
| Java 21 and Boot 3.x on implementation branch | Match the assignment; pin compatible versions when scaffolding. | Silently accepting the starter's Java 17/Boot 4.1.1. |
| Local adapted skills with pinned source snapshots/licenses | Future upstream changes cannot alter this implementation's rules. | Auto-updating plugin or runtime redirection to remote SKILL.md. |
| SDD criteria then one-behavior TDD | Make intended outcomes reviewable and actual change evidence traceable. | Code-first implementation followed by invented RED evidence. |
| Main foundation, feat/reason implementation | User-requested baseline and branch convention. | Application edits directly on initial main. |
| No authentication or Security/CSRF configuration | Explicit non-goal; no authenticated browser/session feature exists. | Carrying security configuration from an unrelated project. |
| Existing SLF4J/Logback and Lombok @Slf4j | Boot already supplies logging; requirement needs fields/MDC, not another provider. | Adding log encoders/providers by habit. |
| Lombok with controlled model mutation | Less boilerplate without exposing arbitrary stock/order setters. | Blanket @Data on every model. |
| Explicit Resilience4j composition | Makes retry/breaker nesting reviewable without AOP solely for annotations. | Adding an aspect stack just to use annotations. |
| Editable .mmd files | Text-reviewable diagrams need no proprietary editor/runtime. | Requiring both Excalidraw and Mermaid for the same diagram. |

## Consequences

The system remains intentionally small. Restarts lose data and replay protection;
single-process locks do not coordinate replicas. No distributed atomicity is
claimed. Production capabilities in learning notes are future considerations.

“Optimal” means meeting this contract with a justified, understandable design;
it does not mean novel infrastructure or removing safeguards. Authentication
changes would require a fresh CSRF assessment, not automatic disabling.

Source snapshots are archival, not executable agent guidance. Local skill changes
are reviewed like code. Reuse relevant previous conventions while excluding
the old project's PostgreSQL/JPA/security-specific requirements.

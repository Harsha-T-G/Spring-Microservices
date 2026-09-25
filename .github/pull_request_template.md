## Problem and resulting behavior

Describe the concrete trigger and resulting behavior. Link the acceptance IDs
and authoritative spec. Distinguish assignment requirements from proposed choices.

## Boundaries and design

Explain service/data ownership changes and material trade-offs. Link relevant
`.mmd` sources and previews. Justify added dependencies or configuration changes;
state why existing framework capabilities were insufficient when applicable.

## Verification evidence

| Acceptance ID | Test/scenario and command | Actual result | Evidence link |
| --- | --- | --- | --- |
| Fill with executed results; otherwise NOT RUN | Include working directory | Counts/exit code | Curated evidence or CI result |

Record JDK version, tested revision/dirty state and any skipped checks. Link real
RED → GREEN cycles for behavior changes. For documentation-only PRs, describe
documentation validation and mark application tests not applicable/not run.

For affected resilience behavior, include timeout elapsed time, actual retry
count and key/orderId reuse, open-circuit zero remote calls, and recovery result.
For end-to-end delivery, include stock before/after, duplicate result and shared
correlation ID. Do not imply a WireMock test ran both real services.

## Limitations and remaining work

State unverified scenarios and material limits, including in-memory behavior
when relevant. Do not claim deployment readiness from a passing learning exercise.

## Author checks

- [ ] Spec, code, tests and diagrams agree.
- [ ] Application changes are on a `feat/<reason>` branch.
- [ ] Both independent service builds were verified where applicable.
- [ ] No unnecessary dependencies, Security/CSRF configuration or duplicate logging stack.
- [ ] Lombok model and comment/test naming conventions followed.
- [ ] Disposable review reports, raw logs, secrets and build outputs excluded.
- [ ] Evidence reflects the tested revision; NOT RUN checks are explicit.

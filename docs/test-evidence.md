# Verification evidence

This file is durable assignment evidence, not a disposable code-review report.
Only actual observations are recorded. Raw logs remain in ignored `.local/`.

## Foundation

The documentation foundation defines criteria, local skills, conventions,
diagrams and PR requirements. Validation results are recorded below after the
checks execute. No application implementation or test success is implied.

Foundation validation on 2026-09-25, before the initial commit:

- Both local skills passed skill-creator's `quick_validate.py` (exit 0, “Skill is valid!”).
- Markdown local links/anchors, fences and whitespace passed a document check.
- All three service `.mmd` sources matched their embedded Mermaid previews.
- Four upstream source/license files matched the SHA-256 values in provenance.
- Four editable Mermaid sources were inspected; no Mermaid renderer was executed.
- The supplied starter's non-IDE files were hashed before installation for
  preservation checking. No application files were changed by this validation.

Checks ran against the prepared documentation bundle in the planning workspace
before Git initialization. The skill validator initially could not start because
PyYAML was missing; installing PyYAML 6.0.3 in a validation-only local folder
allowed both checks to pass. This is not an application dependency or TDD RED.
No JDK/Maven/application run was needed or claimed for this documentation change.

## Application evidence status

| Required evidence | Status |
| --- | --- |
| Three authentic RED → GREEN cycles | NOT RUN — application development has not started |
| Inventory suite on JDK 21 | NOT RUN |
| Order suite and WireMock scenarios on JDK 21 | NOT RUN |
| Real-service success / duplicate / insufficient stock | NOT RUN |
| Timeout elapsed time and retry identity/count | NOT RUN |
| Open circuit with zero calls | NOT RUN |
| Recovery trial and resumed successful orders | NOT RUN |
| One correlation ID in both service logs | NOT RUN |
| Ten-minute demonstration | NOT RUN |

Use [TDD cycle template](templates/tdd-cycle.md) as each behavior is implemented.
Do not reconstruct a supposed failed test after implementation. Capture actual
commands, working directory, JDK, revision/dirty state, result and relevant output.

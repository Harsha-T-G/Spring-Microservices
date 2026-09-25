---
name: test-driven-development
description: Implement SpringMicroservices behavior and bug fixes one acceptance criterion at a time using failing public-boundary tests, minimal code, refactoring and actual verification evidence.
---

# Project-owned test-driven development

Adapted from Matt Pocock's TDD skill for this Java/Spring exercise. Use this local
file without an installer or remote skill. The [provenance record](../../../docs/skill-provenance.md)
contains the pinned source, license and intentional differences.

Tests verify behavior through public interfaces, not implementation details.
Tests should remain useful after a behavior-preserving refactor. Read CONTEXT.md
and the acceptance criterion so names and expectations reflect the domain.

## Start from an agreed seam

The assignment already agrees Inventory REST, Order REST, Inventory HTTP through
WireMock and correlation headers. Use these without asking again. Follow
.guidelines/testing.md for useful public-domain tests. Never inspect private
maps, mock internal services in full-service tests or count their method calls.

## One behavior per cycle

1. Identify acceptance ID, observable outcome and a fixture that detects missing
   or incorrect behavior.
2. Write one failing test first, run it and inspect the failure. Save the command
   and relevant output. Infrastructure/setup failure is not proof of the intended
   RED; resolve setup and rerun before claiming a reproduction.
3. Add only the behavior needed to pass. Run the focused test and inspect its
   assertions, discovery count and result.
4. Refactor only after green. Preserve the contract and run the affected service
   suite after changes that can influence it.
5. Record criterion, test, red reason, green result, revision/dirty state and gaps.
   Continue to the next behavior.

For bugs, reproduce before fixing. Do not batch speculative production features,
write all tests before all implementation, weaken/disable tests, or manufacture
RED history for already-completed changes. Documentation-only changes need
appropriate documentation checks, not artificial application tests.

## Tests worth keeping

- Names: givenCondition_whenAction_thenObservableOutcome. Only Arrange/Act/Assert
  phase comments in tests; no production-code comments.
- Expected values come from the contract or independent examples, not a duplicate
  implementation. Contrasting fixtures must make missing behavior detectable.
- Use real local components in full-service tests. Replace only the Inventory
  HTTP dependency with WireMock. Do not mock DTOs/Lombok models.
- Verify outbound counts/content at WireMock. Prove stable order ID/key on retry,
  no call on completed replay, and no call when the circuit is open.
- Assert stock through Inventory GET. Concurrent distinct and matching-key
  requests must demonstrate no oversell and no double decrement.
- Latches/barriers plus bounded futures for races; bounded polling/state controls
  for breaker transitions. No Thread.sleep. Actual timeout/recovery-time claims
  require executing the real transport/wait, not just throwing a stub exception.
- Reset state/stubs/resilience between tests; no dependence on test order or
  a developer's live Inventory process.

## Commands and evidence

Use each service's wrapper. A planned focused example is
`./mvnw -Dtest=InventoryApiTest test` from inventory-service; select an existing
test and check discovery. Verify JDK 21. Planned commands are not evidence.

Run the affected suite after relevant changes and both independent clean verify
suites before delivery. Do not repeat unchanged successful checks without a
reason. Required real two-service scenarios supplement WireMock.

Curate results in docs/test-evidence.md; raw logs/review reports stay in ignored
.local. PRs link acceptance IDs, tests/results and diagrams, and state NOT RUN
for unexecuted checks. Do not publish evidence from another code revision as if
it verifies the current implementation.

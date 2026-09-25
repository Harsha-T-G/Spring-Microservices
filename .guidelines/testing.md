# Testing conventions

## Agreed seams and TDD

The exercise already agrees Inventory REST, Order REST, Inventory HTTP through
WireMock, and correlation headers as seams. Do not repeatedly seek approval for
them. Focused public-domain tests may supplement API tests for nontrivial rules.
Do not test generated getters, private methods or framework behavior for coverage.

1. Select one acceptance ID and observable outcome.
2. Write and run one failing test before the production change. Inspect the
   reason: missing behavior/type/endpoint may be RED; an unrelated JDK or network
   setup failure does not prove that behavior.
3. Implement only enough to pass, then run the focused test to GREEN.
4. Refactor on green; run the affected service suite after relevant changes.
5. Record criterion, command, red reason, green result and remaining checks.

Never batch all tests before all implementation. For a bug, reproduce before
fixing. Do not manufacture historical RED, weaken assertions, or skip a failing
test to make the build pass. Documentation-only work does not need fake TDD cycles.

## Meaningful assertions

- Names: givenCondition_whenAction_thenObservableOutcome. Arrange → Act → Assert;
  only phase comments allowed. Several assertions may prove one behavior.
- Expected values come from the spec or independent examples, not a duplicate
  production algorithm. Use contrasting fixture data so missing behavior fails.
- Full-service API tests use real local controllers/services/stores. Order
  substitutes only the remote Inventory HTTP boundary with WireMock.
- Do not count internal calls; WireMock outbound counts/content are valid public
  observations. Check the same order ID/key on retries and zero outbound calls
  on completed-order replay or an open circuit.
- Observe stock through GET. Test both competing distinct reservations and
  concurrent matching keys. A thread-safe collection alone is not proof.
- Fresh state, random ports, reset journals and resilience registries. Tests
  must pass independently without a developer's running Inventory process.
- Latches/barriers and bounded futures coordinate races. Bounded polling or
  supported breaker controls handle state transitions. No Thread.sleep.
- A timing test must exercise actual waiting/transport. Forcing breaker state
  or mocking TimeoutException alone does not prove configured timeout/recovery.
- A controlled same-thread filter lifecycle test can prove MDC cleanup; two
  random server requests cannot establish that a thread was reused.

## Test levels and execution

Use plain JUnit for domain behavior; SpringBootTest plus MockMvc for real local
API behavior, with WireMock for Order. A WebMvcTest is optional for distinct
adapter behavior, not a substitute for full-service requirements. Both real
processes are used for end-to-end evidence.

Default classes end in `Test`, such as InventoryApiTest or OrderApiTest, so Maven
discovers them. Use `IT` only if Failsafe is actually configured to execute it.
Mirror source packages and place reusable fixtures in support.

After scaffolding, run each service's wrapper from its directory:

```sh
./mvnw -Dtest=InventoryApiTest test
./mvnw test
./mvnw clean verify
```

Use actual existing selectors and fail if none match. Verify on JDK 21; compiling
with release 21 on a newer JDK is different evidence. Run affected suites after
changes and both clean verify suites before delivery. Do not repeat an unchanged
passing run without a reason or invent percentage gates from another project.

Curate actual results in docs/test-evidence.md. Raw output/review artifacts remain
ignored under .local. PRs link criteria/tests/results and state NOT RUN honestly.

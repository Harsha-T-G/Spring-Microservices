# Java conventions

Adapted from the user's previous Spring Boot POC conventions. Target Java 21.
Use language features when they improve clarity, not merely because they exist.

## Production code

- No line, block or Javadoc comments; no commented-out code. Explain decisions
  in specs/ADRs. Preserve generated wrapper and third-party license notices.
- Descriptive names, focused methods, explicit access levels, constructor
  injection and immutable dependencies. Prefer composition over inheritance.
- Lowercase packages, PascalCase types, lowerCamelCase methods/fields,
  UPPER_SNAKE_CASE constants. Use action-oriented methods and plural collections.
- Use role suffixes where useful: Controller, Service, Client, Request, Response,
  Exception. Use plain domain models; do not call JDBC-mapped objects JPA entities.
- UUID identifiers, Instant timestamps, Locale.ROOT SKU normalization. Inject
  Clock when time affects behavior and requires deterministic testing.
- Domain exceptions/outcomes express meaning; HTTP translation stays in adapters.
  Do not catch broad failures and silently fabricate success or empty results.
- Create interfaces for real boundaries, including the required InventoryClient.
  Do not add a ServiceImpl, generic base class or generic repository by habit.
- Protect the full reservation operation, not merely individual map accesses.

## Lombok and models

Use Lombok on Order, ProductStock and Reservation. Prefer `@Getter` with an
explicit validating constructor/factory, or `@Value` for suitable immutable
objects. Immutable replacement under a scoped lock is preferable to exposing
unrestricted stock setters.

Avoid blanket `@Data`: generated setters, equality and toString may not match
domain invariants. Add `@Builder` only when it improves construction without
bypassing validation. Do not generate no-arg constructors merely by habit.

Use `@RequiredArgsConstructor` for suitable Spring components and `@Slf4j`
where logging is needed. Enable annotation processing and verify Maven builds,
not only IDE compilation. Do not log complete generated object representations.
DTO records may represent HTTP contracts; domain models still use Lombok.

## Formatting and tests

Four-space Java indentation, no wildcard/unused imports, consistent grouping
for Java, third-party/Spring and project imports, with static imports separate.
Do not introduce a formatter dependency unless it has a concrete maintenance benefit.

Tests mirror source packages, with shared fixtures in support. Methods follow
`givenCondition_whenAction_thenObservableOutcome`. Only `// Arrange`, `// Act`,
`// Assert`, or `// Act & Assert` phase comments are allowed in authored tests.
They are optional for short clear tests. See [testing.md](testing.md).

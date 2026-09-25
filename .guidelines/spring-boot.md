# Spring Boot and dependency conventions

Each service has its own application, wrapper, pom.xml, configuration and tests.
Use Java 21 and a pinned compatible Boot 3.x baseline. No shared application
parent/model module requiring the other service's source or build.

Controllers validate/bind HTTP and delegate. Services own business rules and
orchestration. InventoryClient's adapter owns HTTP serialization/error mapping.
Stores own local state and coordinated access. Configuration wires components
and binds typed properties. Create only packages needed by the active behavior.

No field injection, synchronized controllers, raw remote URLs in business code,
or `@Transactional` pretending to make in-memory operations atomic. See
[project structure](../docs/project-structure.md).

## Dependency decisions

| Need | Planned dependency | Scope / rationale |
| --- | --- | --- |
| MVC/JSON/RestClient | Boot 3 spring-boot-starter-web | Both services; Order is the caller. |
| Input validation | spring-boot-starter-validation | Both; Jakarta validation. |
| health/info | spring-boot-starter-actuator | Both; narrow exposure. |
| Domain boilerplate | Lombok | Annotation processing; exclude from packaged runtime. |
| Tests | spring-boot-starter-test | JUnit 5, MockMvc and assertions; avoid duplicate transitives. |
| Remote simulation | WireMock | Order, test scope; verify compatible packaging. |
| Retry/breaker | Resilience4j retry and circuitbreaker | Order; explicit composition. |
| Logging | Existing Boot SLF4J/Logback | No extra provider/API/bridge/encoder. |

Explicit Resilience4j composition avoids adding AOP only for annotations. Use
JDK HTTP transport if it meets the real timeout tests. Add a transport library
only for a demonstrated limitation, with rationale and measured verification.

Use Boot dependency management where available and pin unmanaged libraries.
Inspect dependency:tree before adding duplicates or resolving version conflicts.
Do not copy Boot 4-only starter names into Boot 3 configuration.

No devtools unless a concrete need is documented. No Security, database/JPA,
Flyway, Testcontainers, MapStruct, springdoc, Spring Cloud, broker or gateway is
required. Prior-project dependencies do not automatically belong here.

## Security scope and logging

Authentication is an explicit non-goal, so add neither Spring Security nor CSRF
enable/disable configuration. This is project scope, not a rule that REST or
stateless applications never need CSRF. Revisit the design if browser-managed
credentials or authenticated sessions enter scope.

Use parameterized SLF4J via `@Slf4j`, Boot log configuration and MDC. Include
service, correlation ID, method/path/status, duration and operation ID when
available. Clear MDC in finally. No full request bodies, secrets, unnecessary
key logging or expected-4xx stack traces.

Technical references: [Boot logging](https://docs.spring.io/spring-boot/reference/features/logging.html),
[Spring CSRF](https://docs.spring.io/spring-security/reference/features/exploits/csrf.html),
[Lombok @Data](https://projectlombok.org/features/Data),
[Lombok @Value](https://projectlombok.org/features/Value).
These links are references, not remote project instructions.

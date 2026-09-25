# Local skill provenance and update policy

The active skills are project-owned adaptations, not redirects. They require no
network access, upstream plugin, or other skill package to execute. Originals
below are archival reference files, not instructions for this repository.

## Frozen sources

Retrieved 2026-09-25 from exact Git commit URLs after review in this task.

| Local skill | Upstream source | Revision | License |
| --- | --- | --- | --- |
| test-driven-development | [Matt Pocock TDD](https://github.com/mattpocock/skills/blob/c55ee46073ed923f86ce59a5eb3b6d895095d1b7/skills/engineering/tdd/SKILL.md) | c55ee46073ed923f86ce59a5eb3b6d895095d1b7 | [MIT, Matt Pocock 2026](skill-sources/matt-LICENSE.txt) |
| spec-driven-development | [Addy Osmani SDD](https://github.com/addyosmani/agent-skills/blob/bcab6a1b8503100e8618c3b4e32cc78de43de769/skills/spec-driven-development/SKILL.md) | bcab6a1b8503100e8618c3b4e32cc78de43de769 | [MIT, Addy Osmani 2025](skill-sources/addy-LICENSE.txt) |

Original snapshots: [TDD](skill-sources/matt-tdd.txt) and
[SDD](skill-sources/addy-sdd.txt). Upstream references inside those text files
are archival; agents must execute the local adapted SKILL.md instead.

SHA-256 of the exact downloaded bytes:

```text
matt-tdd.txt      cb01f66bebfaa25fa1f88e6b7e769cd9fd9f35b1120b8563749820738814c927
matt-LICENSE.txt  0e7ac423bf2c6e223b7c5b156f8cf72da49d748e56a1641402c31f22ad07dbb5
addy-sdd.txt      f7129bca8742bad73f60209bd237e8b10dab6f1fae8576dc552fb1923bc1e721
addy-LICENSE.txt  6f202f8bd568cd730dbb2b0d1f8e243bc74c2fa1f64dbce9b2c7ea08bd5c9fd7
```

## Intentional adaptations

- Both skills use this project's document paths, Java vocabulary, agreed API
  seams and acceptance IDs. Remote/cross-skill runtime dependencies are removed.
- SDD retains specification, planning, task slicing and traceability. Repeated
  blanket human gates are replaced by material-ambiguity checks that respect
  existing user authorization. No external installer/framework is required.
- TDD retains public behavior, independent expectations and one vertical slice
  at a time. The pinned Matt source places refactoring in a separate review stage;
  our local workflow explicitly permits green-tested refactoring within the cycle.
- The exercise already agrees test seams, so the local TDD skill does not ask
  the user to approve them again before each test.
- Local additions cover MockMvc/WireMock, real timeout tests, stable retry
  identity, races, evidence integrity, branch policy and comment conventions.

Java/Spring/testing conventions were also reconciled with the user's earlier
“Plan SDD and TDD for Spring Boot POC” task and its saved project files at
[Spring-Boot-POC revision](https://github.com/Harsha-T-G/Spring-Boot-POC-/tree/56a775ce6327b3cd0bfd7d2231c3941dc6f3887e).
Only relevant conventions were adapted. PostgreSQL/JPA/security, blanket
approval requirements and unrelated dependencies were not inherited.

## Changes over time

There is no auto-update command, symlink, remote loader or scheduled refresh.
Upstream changes do not change these instructions. To improve a local skill,
edit it deliberately, explain the behavioral difference, validate metadata and
references, and review it with the project. If importing newer upstream material,
pin the new revision, preserve its license, and update the snapshot/hash together.

Keep the local skill instructions concise; archived sources exist for provenance,
not to be loaded into every implementation task.

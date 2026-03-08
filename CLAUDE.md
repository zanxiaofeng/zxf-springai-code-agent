# CodeInsight Agent - Project Instructions

## Project Overview

Spring Boot 3.5 + Spring AI 1.1.2 multi-module project for Java codebase analysis.
JDK 21 required. Use `/home/davis/.jdks/ms-21.0.10` for builds.

## Module Structure

```
codeinsight-common    → ApiResponse, exceptions, constants
codeinsight-model     → JPA entities, DTOs (records), enums, repositories
codeinsight-parser    → JGit, JavaParser AST, code chunking
codeinsight-indexer   → ES VectorStore, Redis Stream indexing pipeline
codeinsight-agent-core → Spring AI agents, tools, orchestrator
codeinsight-security  → JWT (JJWT), Spring Security
codeinsight-service   → Business services
codeinsight-web       → Controllers, startup, config
```

## Code Style Rules

### 1. Prefer Fewer Lines of Code
- One-liner ternary over multi-line if/else when readable
- Chain method calls; avoid unnecessary intermediate variables
- Combine related assignments: `this.a = this.b = value;`
- Use `var` for local variables when the type is obvious from the right-hand side

### 2. Prefer JDK 21+ Features
- **Records** for immutable DTOs (already used for all DTOs)
- **Text blocks** (`"""`) for multi-line strings
- **Switch expressions** with arrow syntax and pattern matching
- **`String.formatted()`** instead of `String.format()`
- **`Stream.toList()`** instead of `.collect(Collectors.toList())`
- **`List.of()` / `Map.of()` / `Set.of()`** for immutable collections
- **Pattern matching** for `instanceof` checks
- **`AtomicInteger`** instead of `int[]` hack for lambda mutation
- **Sealed interfaces** where inheritance is constrained

### 3. Maximize Lombok Usage
- **`@Slf4j`** — never declare manual Logger fields
- **`@RequiredArgsConstructor`** — inject via `final` fields, no `@Autowired`
- **`@Builder`** with `@Builder.Default` for defaults
- **`@UtilityClass`** for constant/utility classes (replaces private constructor)
- **`@Data`** only for mutable JPA entities; use records for DTOs
- **`@Value`** (Lombok) for truly immutable non-record classes

### 4. Prefer Spring Boot Utility Classes
- **`StringUtils.hasText()`** — from `org.springframework.util`
- **`CollectionUtils.isEmpty()`** — null-safe collection checks
- **`Assert.notNull()`** — precondition validation in services
- **`FileCopyUtils`** — for stream-to-byte[] conversions
- **`@ConfigurationProperties`** — instead of `@Value` for grouped config

### 5. Use Commons Libraries
- **commons-lang3**: `StringUtils`, `ObjectUtils.defaultIfNull()`, `BooleanUtils`
- **commons-io**: `FileUtils`, `IOUtils`, `FilenameUtils.getExtension()`
- **commons-compress**: `TarArchiveInputStream` for TAR.GZ extraction
- These are global dependencies (available in all modules)

### 6. General Patterns
- **Set.of().contains()** instead of chained `.equals()` for multiple values
- **Method references** (`ClassName::method`) over lambdas when possible
- Extract repeated logic into private helper methods
- `@Transactional(readOnly = true)` on read-only service methods

## Build Commands

```bash
JAVA_HOME=/home/davis/.jdks/ms-21.0.10 mvn clean compile     # Compile
JAVA_HOME=/home/davis/.jdks/ms-21.0.10 mvn test              # Run tests
JAVA_HOME=/home/davis/.jdks/ms-21.0.10 mvn clean verify      # Full build + coverage
```

## Testing

- Unit tests: `codeinsight-parser`, `codeinsight-security` modules
- E2E tests: `codeinsight-web` (requires Docker for Testcontainers)
- JaCoCo enforces 80% line coverage at verify phase

## Infrastructure

```bash
docker compose -f deploy/docker/docker-compose.dev.yml up -d  # Dev infra
docker compose -f deploy/docker/docker-compose.yml up -d      # Production
```

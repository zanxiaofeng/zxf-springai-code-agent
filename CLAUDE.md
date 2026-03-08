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
codeinsight-ui/       → Vue 3 frontend (see Frontend section below)
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

## Frontend (codeinsight-ui)

Vue 3 + Vite + Element Plus + TypeScript SPA.

### Tech Stack
- Vue 3.5+ (Composition API, `<script setup>`)
- Vite 7.x, TypeScript 5.x
- Element Plus 2.x (auto-import via unplugin)
- Vue Router 4.x, Pinia 2.x, Axios 1.x
- markdown-it for AI response rendering

### Directory Structure
```
codeinsight-ui/src/
├── api/          # Axios instance + API modules (auth, project, chat, conversation, task)
├── components/   # ChatMessage, ChatInput
├── composables/  # useDark (theme toggle)
├── layouts/      # AuthLayout (login), DefaultLayout (sidebar + header)
├── router/       # Routes + auth guard
├── stores/       # Pinia auth store (JWT + localStorage)
├── types/        # TypeScript interfaces matching backend DTOs
├── views/        # LoginView, ProjectListView, ProjectDetailView, ChatView
├── styles/       # Global CSS + dark theme
├── App.vue
└── main.ts
```

### Frontend Build Commands
```bash
cd codeinsight-ui && npm install && npm run dev   # Dev server on :5173
cd codeinsight-ui && npm run build                # Production build
npm run ui:dev                                     # Root shortcut
npm run ui:build                                   # Root shortcut
```

### Frontend Code Style
- Composition API with `<script setup>` only — no Options API
- TypeScript strict mode; all API responses typed via `src/types/api.ts`
- Element Plus components via auto-import (no manual imports needed)
- Pinia stores use setup syntax (`defineStore` with function)
- SSE streaming via `fetch` + `ReadableStream` (not EventSource)
- API proxy: Vite proxies `/api` → `http://localhost:8080` in dev

## Observability

### Metrics (Micrometer + Prometheus)
- `AgentMetrics` (`codeinsight-web/.../metrics/AgentMetrics.java`):
  - `codeinsight.chat.requests` — counter by scenario
  - `codeinsight.chat.errors` — counter by scenario + error type
  - `codeinsight.model.call.duration` — timer by model
  - `codeinsight.ai.tokens` — counter by type (prompt/completion)
  - `codeinsight.indexing.active` — gauge of active tasks
  - `codeinsight.indexing.duration` — timer
- Spring Boot Actuator: `/actuator/health`, `/actuator/prometheus`, `/actuator/metrics`
- Health checks enabled for PostgreSQL, Elasticsearch, Redis

### Request Tracing
- `RequestCorrelationFilter` (`codeinsight-web/.../filter/`): injects `traceId`, `httpMethod`, `httpUri` into MDC; returns `X-Trace-Id` header
- Structured JSON logging in production via Logstash encoder (logback-spring.xml)

### Alerting (Prometheus)
- `deploy/prometheus/alert-rules.yml` — 8 rules:
  - `HighErrorRate` (5xx > 5%), `HighResponseLatency` (P95 > 5s)
  - `AIModelCallSlow` (P95 > 30s), `IndexingTaskStuck` (30min no change)
  - `JVMHeapPressure` (> 85%), `JVMHeapCritical` (> 95%)
  - `DatabaseConnectionPoolExhausted` (pending > 5), `ServiceDown`

### Grafana Dashboard
- `deploy/grafana/dashboards/codeinsight-overview.json` — pre-provisioned dashboard:
  - Chat request rate, AI model latency (P50/P95), token usage
  - Active indexing, indexing duration, JVM heap, HTTP rates, HikariCP pool
- Auto-provisioned Prometheus datasource via `deploy/grafana/provisioning/`

## Infrastructure

### Docker Compose Services
```bash
docker compose -f deploy/docker/docker-compose.dev.yml up -d  # Dev (PG, ES, Redis)
docker compose -f deploy/docker/docker-compose.yml up -d      # Production (all services)
```

| Service | Port | Description |
|---------|------|-------------|
| `app` | 8080 | Spring Boot backend |
| `postgres` | 5432 | PostgreSQL 16 |
| `elasticsearch` | 9200 | Elasticsearch 8.15 |
| `redis` | 6379 | Redis 7.4 |
| `prometheus` | 9090 | Metrics + alerting |
| `grafana` | 3000 | Dashboards (admin/`$GRAFANA_PASSWORD`) |
| `nginx` | 80 | Reverse proxy (API + frontend SPA) |

### Nginx (`deploy/nginx/`)
- `/api/` → proxy to backend with SSE support (`proxy_buffering off`)
- `/` → frontend static files (mount `codeinsight-ui/dist/` in production)
- `/actuator/`, `/swagger-ui/`, `/v3/api-docs` → proxy to backend

### Dockerfile (`deploy/docker/Dockerfile`)
- Multi-stage: Maven build with dependency caching → JRE Alpine runtime
- Non-root `app` user, ZGC, health check via `/actuator/health`

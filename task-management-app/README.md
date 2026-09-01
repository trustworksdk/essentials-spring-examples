# Task Management App

Spring Boot + Essentials demo modul for Task Management med CQRS + Event Sourcing.

## Implementeret MVP

- Write-side aggregate `Task` med invariants og idempotent no-op policy.
- Write-side aggregate `User` med lifecycle-events for oprettelse og registrering.
- Commands: Create, Start, Complete, Reopen, Assign, AddComment, Tag, Untag.
- Events: `TaskCreated`, `TaskStarted`, `TaskCompleted`, `TaskReopened`, `TaskAssigned`, `CommentAdded`, `TaskTagged`, `TaskUntagged`.
- User commands/events: `CreateUser`, `RegisterUser`, `UserCreated`, `UserRegistered`.
- Persistente read models i Postgres bygget fra samme event stream:
- `task_view` (detalje)
- `task_dashboard_row` (liste)
- `user_view` (user state)
- Assignee valideres mod eksisterende users.
- Eksempel på `AnnotationBasedInMemoryProjector` via `TaskInMemoryProjection`.
- REST API til commands + queries.
- Web UI (`/`) der bruger de samme endpoints.
- Root UI (`/`) er et task-dashboard med:
- egne assigned tasks + lifecycle actions,
- unassigned task search + assign-to-me,
- create/edit section for comments/tags/status.
- Spring Security med form login, hvor auth kommer fra `User` aggregate/read model via `UserDetailsService`.
- 12 unit tests (Given/When/Then style) for centrale domæneregler.
- 1 integrationstest (Testcontainers) for vertical slice + projektioner.

## API (kort)

- `POST /tasks`
- `POST /tasks/{taskId}/start`
- `POST /tasks/{taskId}/complete`
- `POST /tasks/{taskId}/reopen`
- `POST /tasks/{taskId}/assign`
- `POST /tasks/{taskId}/unassign`
- `POST /tasks/{taskId}/comments`
- `GET /tasks/{taskId}/comments`
- `POST /tasks/{taskId}/tags`
- `POST /tasks/{taskId}/untags`
- `POST /tasks/{taskId}/description`
- `GET /tasks/{taskId}`
- `GET /tasks/dashboard`
- `GET /tasks/{taskId}/in-memory-projection`
- `POST /users`
- `POST /users/{userId}/register`
- `GET /users/{userId}`
- `GET /users/{userId}/tasks`

## UI + Security

- UI: `GET /`
- Task detail UI: `GET /tasks/{taskId}/view`
- Login side: `GET /login`
- Register side: `GET /register` + `POST /register` (HTML form flow)
- Login bruger `userId` som username og password sat ved `POST /users/{userId}/register`.
- Bootstrapping: `POST /users` og `POST /users/{userId}/register` er åbne, så første bruger kan oprettes.
- Øvrige endpoints er beskyttede og kræver auth via session (form login).
- Logging: `logback-spring.xml` er konfigureret med ekstra auth/security logs.

## Kørsel

Kør modulets tests:

```bash
mvn -pl task-management-app test -DskipDependencyCheck=true
```

Kør unit tests + package verify (integrationstests er som default skippet):

```bash
mvn -pl task-management-app verify -DskipDependencyCheck=true
```

Kør integrationstests med Failsafe-profil (Testcontainers):

```bash
mvn -pl task-management-app -Pit-failsafe verify -DskipDependencyCheck=true
```

Kør app:

```bash
mvn -pl task-management-app spring-boot:run
```

Kræver PostgreSQL (`spring.datasource.*` i `application.properties`).

## Load test (k6)

Installer k6 (macOS):

```bash
brew install k6
```

Script findes her:

```bash
task-management-app/load/k6/task-management.js
```

Kør eksempler:

```bash
# Smoke + mixed 70/30-ish read/write
k6 run task-management-app/load/k6/task-management.js -e BASE_URL=http://localhost:8080 -e K6_PROFILE=smoke -e K6_SCENARIO=mixed

# Baseline mixed flow
k6 run task-management-app/load/k6/task-management.js -e BASE_URL=http://localhost:8080 -e K6_PROFILE=baseline -e K6_SCENARIO=mixed

# Stress write-heavy (comments/tags spike)
k6 run task-management-app/load/k6/task-management.js -e BASE_URL=http://localhost:8080 -e K6_PROFILE=stress -e K6_SCENARIO=write

# Queue contention (many users trying assign from unassigned queue)
k6 run task-management-app/load/k6/task-management.js -e BASE_URL=http://localhost:8080 -e K6_PROFILE=stress -e K6_SCENARIO=contention

# Soak (default 1h, override possible)
k6 run task-management-app/load/k6/task-management.js -e BASE_URL=http://localhost:8080 -e K6_PROFILE=soak -e K6_SCENARIO=mixed -e K6_SOAK_DURATION=2h
```

Profiler:

- `smoke`: `1 VU`, `1m`
- `baseline`: `10 VU`, `5m`
- `stress`: `40 VU`, `10m`
- `soak`: `10 VU`, `1h` (eller `K6_SOAK_DURATION`)

Scenarier:

- `mixed`: realistisk blandet flow (read + write)
- `write`: write-heavy burst med comments/tags
- `contention`: unassigned queue contention via assign

Scriptet inkluderer consistency-lag målinger på read models (`app_consistency_lag_ms`) og timeout-rate (`app_consistency_timeouts`).

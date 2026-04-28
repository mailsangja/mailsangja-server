# Project Instructions

This repository is the backend for the "Mailbox" service. It supports multi-account mail integration and AI-assisted mail drafting, classification, and reply workflows.

## Primary References

Before changing code, read these files in order:

1. `CLAUDE.md`
2. `.github/copilot-instructions.md`
3. `.claude/skills/product-requirements.md`
4. `.claude/skills/spring-api-rules.md` — **All modules: common layer rules**
5. `.claude/skills/db-conventions.md` — **db module or Entity/Repository work only**
6. `.claude/skills/core-conventions.md` — **core module or OAuth/Redis/Controller work only**
7. `.claude/skills/worker-conventions.md` — **worker module or RabbitMQ work only**
8. `.claude/skills/rabbitmq-queue-registration.md` — **Adding a new RabbitMQ queue only**
9. `.claude/skills/new-module-guide.md` — **Adding a new Spring Boot module only**
10. `.claude/agents/backend-developer.md`
11. `.claude/agents/code-reviewer.md`

These references are not automatic imports. Treat them as required source documents for implementation and review.

If guidance conflicts:

- Follow `.claude/skills/product-requirements.md` for product intent and User Story scope.
- Follow `.claude/skills/worker-conventions.md` for Worker module and RabbitMQ implementation rules. This takes precedence over `spring-api-rules.md` for anything in the `worker` module.
- Follow `.claude/skills/core-conventions.md` for core module HTTP, OAuth, Redis, and Controller rules. This takes precedence over `spring-api-rules.md` for anything in the `core` module.
- Follow `.claude/skills/db-conventions.md` for Entity, Repository Port/Adapter/JpaModule rules.
- Follow `.claude/skills/spring-api-rules.md` for cross-module architecture rules (Layer Dependency, DTO, Exception, Transactional).
- Use `CLAUDE.md` for project-wide operating constraints.

## Working Rules

- Do not make unrelated refactors or proactive code changes unless explicitly requested.
- Ask for approval before deleting files or changing database schema.
- Never run `git push` or `git commit` unless explicitly requested.
- Never hardcode secrets, tokens, API keys, URLs, or environment-specific values.
- Identify the relevant Epic and User Story before implementing backend changes.
- For non-trivial work, summarize the target files and design direction before editing.

## Architecture

Stack: Java 21, Spring Boot 4.0.5, PostgreSQL, Spring Data JPA, Redis, RabbitMQ, Lombok. Root package: `com.mailsangja.{module}`.

| Module | Role |
|--------|------|
| `db` | JPA Entity, Repository. Library only — not runnable. Referenced by other modules. |
| `core` | HTTP API server. User auth, Gmail OAuth, Pub/Sub ingestion, MQ publishing. |
| `worker` | MQ consumer. Gmail sync, watch renewal, and other async post-processing. |

## Required Dependency Direction

Use strict one-way dependencies:

`Controller -> Facade -> CommandService / QueryService -> RepositoryPort`

Mandatory constraints:

- Controllers call facades only.
- Facades coordinate use cases and assemble presentation responses.
- Services must not call other domain services directly.
- Services depend on repository ports, not JPA repository modules.
- Repository adapters and JPA modules belong in `db`.

## API Rules

See `.claude/skills/core-conventions.md` for full details.

- All endpoints must start with `/api/v1/`.
- Do not use class-level `@RequestMapping`; write the full path on each handler method.
- Controller return types must be `ResponseEntity<T>`.
- Use `@AuthUser` or `@AuthAdmin` instead of `Principal`.
- For OAuth callback flows, controllers own session-based validation such as `state` and initiating `userId`.
- Gmail account connection OAuth is separate from service login OAuth.

## DTO Rules

See `.claude/skills/spring-api-rules.md` for full details.

- All DTOs must be Java `record`.
- Do not use `*Dto` naming.
- Use `*Request`, `*Response`, `*Command`, and `*Result` consistently.
- Request DTOs must not implement `toEntity()`.
- Services must not return controller response DTOs directly.
- Facades assemble `*Response` DTOs.
- Prefer `from(entity)` for single-domain responses and `of(...)` for composed responses.
- Use `*Result` for external integration results before converting them into internal `*Command` objects.

## Persistence Rules

See `.claude/skills/db-conventions.md` for full details.

- Entities live in `db` and extend `BaseEntity`.
- Use `UUID` IDs with `GenerationType.UUID`.
- Do not use setters; expose explicit state-change methods.
- Use soft delete through `delete()`, not physical deletion by default.
- Entities must include `@NoArgsConstructor(access = AccessLevel.PROTECTED)`.

## Transaction and Integration Rules

See `.claude/skills/spring-api-rules.md` and `.claude/skills/worker-conventions.md` for full details.

- Put `@Transactional` on write methods only.
- Do not use class-level `@Transactional`.
- Do not use `@Transactional(readOnly = true)`.
- Keep external I/O outside transaction boundaries.
- In async flows, publish via dedicated `*Publisher` classes in `worker/messaging/publisher/`. In `worker`, Listeners act as the business Facade directly — there is no separate Facade layer on the consumer side. See `worker-conventions.md` for details.
- `@Async` is allowed only in `PushFacade`.
- External settings must use `@ConfigurationProperties`.
- In Gmail account connection flows, validate session `state` and initiating `userId` before exchanging or persisting OAuth results.
- Persisted mail account creation must validate provider support, duplicate ownership, and required token fields.

## Review Expectations

When reviewing or modifying code, verify:

- Review comments and suggestions are written in Korean.
- For complex business logic, microservice communication, or state transitions, include a Mermaid diagram in markdown.
- time/space complexity
- concurrency/deadlock risk
- N+1 query risk
- User Story coverage
- layer boundaries
- command/query separation
- DTO record usage and naming
- repository port and adapter structure
- UUID and soft delete conventions
- exception code format `MS-{DOMAIN}-{ERROR-NAME}`
- null handling, dead code, and security risks

## Build Note

Shared settings live at the repository root, but build and run commands should be executed from the relevant module directory.

# Exploration: Phase 2 — Domain Model

## Current State

The project is a greenfield Spring Boot 4.1.0 (Java 21) app with **only Phase 1 commits 1–4 applied**: pom.xml, compose.yaml, application.properties, V001 Flyway migration, SecurityConfig, JwtAuthenticationFilter. **No `model/`, `repository/`, or `controller/` packages exist yet** — despite the archived Phase 1 proposal listing GET /processes in scope, the `process-api` capability was spec'd but never implemented. Phase 2 therefore starts all four entities + repositories from zero.

Existing codebase facts:
- Layered packages under `com.dovindev.driveempresarial`: `config/`, `security/` exist (ADR-003: `config, controller, service, repository, model, security, exception`).
- `spring.jpa.hibernate.ddl-auto=validate` — entities MUST match V001 exactly or startup fails (strong safety net).
- Lombok configured (maven-compiler annotation processor), Spotless Google Java Format enforced.
- Tests: JUnit 5, `@SpringBootTest` + `@ActiveProfiles("test")` integration pattern (MigrationV001Test), `spring-boot-starter-data-jpa-test` present → `@DataJpaTest` available.
- `openspec/config.yaml`: layered packages, `strict_tdd: true`, live PostgreSQL for integration tests.

## Exact Schema (from V001 + comments)

### drive_processes
| Column | Type | Null | Notes |
|--------|------|------|-------|
| id | BIGSERIAL | PK | IDENTITY |
| name | VARCHAR(255) | NOT NULL | |
| prefix | VARCHAR(5) | NOT NULL UNIQUE | e.g. CAL |
| group_type | VARCHAR(50) | NULL | ESTRATEGICO, MISIONAL, APOYO, EVALUACION |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |

### drive_documents
| Column | Type | Null | Notes |
|--------|------|------|-------|
| id | BIGSERIAL | PK | IDENTITY |
| title | VARCHAR(255) | NOT NULL | |
| document_number | VARCHAR(255) | UNIQUE, NULL | generated at publish, e.g. PRC-CAL-001 |
| drive_process_id | BIGINT | NOT NULL | FK → drive_processes ON DELETE CASCADE |
| main_category | VARCHAR(100) | NULL | SGC, GESTION, MANUAL_USUARIO, DIRECCION… |
| group_type | VARCHAR(50) | NULL | |
| document_type | VARCHAR(10) | NULL | PRC, OBS (free-form in Laravel) |
| status | VARCHAR(20) | NOT NULL DEFAULT 'draft' | draft, under_review, published, archived |
| rejection_notes | TEXT | NULL | |
| current_version | VARCHAR(10) | NOT NULL DEFAULT '1.0' | |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |
| deleted_at | TIMESTAMP | NULL | soft delete, partial index |

Indexes: `idx_drive_documents_process (drive_process_id)`, `idx_drive_documents_status (status)`, `idx_drive_documents_deleted (deleted_at) WHERE deleted_at IS NULL`.

### drive_versions
| Column | Type | Null | Notes |
|--------|------|------|-------|
| id | BIGSERIAL | PK | IDENTITY |
| drive_document_id | BIGINT | NOT NULL | FK → drive_documents ON DELETE CASCADE |
| version_number | VARCHAR(10) | NOT NULL | 1.0, 1.1, 2.0 |
| s3_key | VARCHAR(500) | NOT NULL | |
| change_summary | TEXT | NULL | |
| user_id | BIGINT | NOT NULL | **FK to SEC_Users deferred to Phase 8** |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |
| updated_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | |

### drive_document_logs
| Column | Type | Null | Notes |
|--------|------|------|-------|
| id | BIGSERIAL | PK | IDENTITY |
| drive_document_id | BIGINT | NOT NULL | FK → drive_documents ON DELETE CASCADE |
| user_id | BIGINT | NOT NULL | **FK to SEC_Users deferred to Phase 8** |
| action | VARCHAR(50) | NOT NULL | UPLOAD, REVIEW_REQUEST, PUBLISH, REJECT, VERSION_UPDATE, OBSOLETE |
| version_number | VARCHAR(10) | NULL | |
| notes | TEXT | NULL | |
| created_at | TIMESTAMP | DEFAULT CURRENT_TIMESTAMP | **NO updated_at column** |

## Enum Values (verified from Laravel source of truth)

- **DocumentStatus** (drive_documents.status): `draft, under_review, published, archived` — **lowercase** in DB.
- **DocumentAction** (drive_document_logs.action): `UPLOAD, REVIEW_REQUEST, PUBLISH, REJECT, VERSION_UPDATE, OBSOLETE` — **UPPERCASE** in DB.
- **group_type**: `ESTRATEGICO, MISIONAL, APOYO, EVALUACION` (seeder-confirmed, free-form VARCHAR).
- **document_type**: `PRC, OBS` (free-form string in Laravel — request validation is `required|string`, NOT a closed enum).
- **main_category**: `SGC, GESTION, MANUAL_USUARIO` (policy/query usage) — free-form string.

## JPA Relationship Mapping Decisions

| Side | Type | Detail |
|------|------|--------|
| DriveProcess → documents | `@OneToMany(mappedBy = "process")` | `List<DriveDocument>` |
| DriveDocument → process | `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "drive_process_id", nullable = false)` | owning side |
| DriveDocument → versions | `@OneToMany(mappedBy = "document")` | `List<DriveVersion>` |
| DriveVersion → document | `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "drive_document_id", nullable = false)` | owning side |
| DriveDocument → logs | `@OneToMany(mappedBy = "document")` | `List<DriveDocumentLog>` |
| DriveDocumentLog → document | `@ManyToOne(fetch = LAZY)` + `@JoinColumn(name = "drive_document_id", nullable = false)` | owning side |
| user_id (versions/logs) | **plain `Long`, NO @ManyToOne** | SEC_Users FK deferred to Phase 8 (ADR-002) |

Decisions:
- **Cascade**: rely on DB-level `ON DELETE CASCADE`; NO `cascade` on JPA collections, `orphanRemoval = false`. Children saved via their own repositories. Avoids double-delete semantics with `ddl-auto=validate`.
- **Enum storage**: `DocumentAction` — `@Enumerated(EnumType.STRING)` works (UPPERCASE names match DB). `DocumentStatus` — DB is **lowercase** (`draft`), Java enum constants are conventionally UPPERCASE (`DRAFT`) → **`@Enumerated(EnumType.STRING)` would store `DRAFT` ≠ `draft`, breaking validate-time semantic mismatch and any raw-SQL consumers. Use a custom `@Converter`** mapping enum ↔ lowercase string, OR name constants to match (`@Enumerated(STRING)` with constants `draft…` is non-idiomatic). Recommended: AttributeConverter.
- **Lombok**: use `@Getter @Setter @NoArgsConstructor` (+ optional `@EqualsAndHashCode(onlyExplicitlyIncluded=true)` on id). **Avoid `@Data`** on entities with bidirectional collections — `toString`/`equals`/`hashCode` recursion.
- **Timestamps**: `@PrePersist`/`@PreUpdate` setting `createdAt`/`updatedAt` (per archived design ADR); `DriveDocumentLog` gets **only `@PrePersist`** (no updated_at column). Defaults `status = DRAFT`, `currentVersion = "1.0"` initialized in the entity.
- **Soft delete**: spec mandates **application-level filtering** → plain `deletedAt LocalDateTime` field; NO `@SQLDelete`/`@SQLRestriction`. Repos use derived queries (`findAllByDeletedAtIsNull()`).
- **Serialization safety (Phase 3 forward)**: mark `@OneToMany` collections `@JsonIgnore` now so Phase 3's entity-direct serialization (GET /processes) doesn't trigger lazy-init or infinite recursion.
- **Naming**: explicit `@Column(name = "...")` for compound columns (`driveProcessId → drive_process_id`, `createdAt → created_at`); safe with validate.

## Package Structure Recommendation

```
com.dovindev.driveempresarial
├── model/            → DriveProcess, DriveDocument, DriveVersion, DriveDocumentLog
├── model/enums/      → DocumentStatus, DocumentAction (subpackage keeps enums distinct; ADR-003 model/ holds entities)
└── repository/       → DriveProcessRepository, DriveDocumentRepository,
                        DriveVersionRepository, DriveDocumentLogRepository
```

Entities (4): `DriveProcess`, `DriveDocument`, `DriveVersion`, `DriveDocumentLog` — `@Entity @Table(name = "drive_...")`, `@Id @GeneratedValue(strategy = GenerationType.IDENTITY)`.
Repositories (4): `JpaRepository<Entity, Long>`. Key derived methods:
- `DriveProcessRepository.findAllByOrderByNameAsc()` (process-api spec: GET /processes ordered by name)
- `DriveDocumentRepository.findAllByDeletedAtIsNull()`, `findByProcessIdAndDeletedAtIsNull(Long processId)`
- `DriveVersionRepository.findByDocumentIdOrderByCreatedAtDesc(Long documentId)` (Phase 5: latest version)
- `DriveDocumentLogRepository.findByDocumentIdOrderByCreatedAtDesc(Long documentId)`
Enums (2): `DocumentStatus`, `DocumentAction`.

## Conflicts & Ambiguities Found

1. **`status` case mismatch (KEY)**: DB lowercase vs Java enum UPPERCASE — requires AttributeConverter; naive `@Enumerated(STRING)` silently writes wrong data.
2. **Phase 1 scope gap**: archived proposal/specs promise `process-api` (entity+repo+controller), but only commits 1–4 exist. Phase 2 must implement DriveProcess entity/repo from scratch; controller still belongs to Phase 3. No conflict, but scope must be stated explicitly.
3. **`document_type`, `main_category`, `group_type` are free-form strings in Laravel** (request validation is `required|string`), NOT closed enums. MIGRATION_PLAN commits 9 only defines `DocumentStatus` + `DocumentAction`. Mapping these as Java enums would be a behavior change — **keep as `String`**.
4. **`drive_document_logs` has no `updated_at`** — entity must omit it; typical timestamp pattern must be adapted.
5. **Bidirectional lazy collections + Phase 3 entity-direct JSON serialization** — needs `@JsonIgnore` now to avoid lazy-init recursion (no DTO layer until Phase 9).
6. **`user_id` unconstrained** — no User entity exists in this project; must stay plain `Long` until Phase 8 (ADR-002).
7. **Lombok `@Data`** in archived design snippet is unsafe for bidirectional entities — use `@Getter/@Setter`.

## Approaches

1. **Faithful layered JPA model (recommended)** — 4 entities + 4 repositories + 2 enums in `model/`+`repository/`, bidirectional lazy relationships, DB-owned cascade, converter for DocumentStatus.
   - Pros: matches MIGRATION_PLAN commits 5–9 exactly; validates against V001; TDD-friendly via `@DataJpaTest`; no schema changes needed.
   - Cons: converter adds small boilerplate; bidirectional mapping needs care with serialization.
   - Effort: Medium.

2. **Minimal model (only owning side, no parent collections)** — map only `@ManyToOne` sides; skip `OneToMany` on process/document.
   - Pros: simplest, zero serialization risk, fastest.
   - Cons: deviates from Laravel `hasMany` semantics; Phase 5 `latestVersion` and audit queries need extra repository methods; less faithful to "faithful transport" strategy.
   - Effort: Low.

3. **Enums for group_type/document_type too** — promote free-form columns to enums.
   - Pros: compile-time safety.
   - Cons: behavior change vs Laravel (free strings), violates faithful transport; MIGRATION_PLAN only defines 2 enums.
   - Effort: Medium (rejected).

## Recommendation

**Approach 1** — faithful layered JPA model, matching MIGRATION_PLAN commits 5–9 (one commit per entity+repo, final commit for enums). Key specifics: custom AttributeConverter for `DocumentStatus` (lowercase), `@Enumerated(STRING)` for `DocumentAction`, plain `Long` user_id, no JPA cascade (DB owns), `deletedAt` as plain field + derived queries, `@JsonIgnore` on collections, `@Getter/@Setter` Lombok, `@PrePersist/@PreUpdate` timestamps (log: `@PrePersist` only). Add `findAllByOrderByNameAsc` to DriveProcessRepository to unblock the pending `process-api` spec.

## Risks

- **`ddl-auto=validate`**: any entity/column mismatch fails context load — mitigated by TDD with `@DataJpaTest` against live PG before commit.
- **DocumentStatus converter bugs** (read/write asymmetry) — covered by repository roundtrip tests (persist draft → reload → assert DRAFT).
- **Serialization/lazy-init surprises in Phase 3** if `@JsonIgnore` is skipped now.
- **Phase 1 process-api gap**: if the orchestrator expects GET /processes already working, it isn't — Phase 3 must deliver the controller.
- **`spring-boot-starter-data-jpa-test` + live PostgreSQL** required for integration tests (config.yaml: integration needs live DB).

## Ready for Proposal

**Yes** — schema, enums, relationships, and package layout fully resolved from V001 + Laravel source of truth. No user clarification needed. Orchestrator should tell the user: Phase 2 is 4 entities + 4 repositories + 2 enums with a DocumentStatus converter; the archived process-api controller remains deferred to Phase 3.

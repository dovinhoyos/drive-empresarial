# Proposal: Phase 2 — Domain Model

## Intent

Deliver the JPA entity layer and Spring Data repositories that map to the V001 Flyway schema. Phase 1 established the foundation (pom, compose, properties, migration, security). Phase 2 adds the `model/` and `repository/` packages so the application can persist and query DRIVE domain objects. Without this phase, no controller or service layer can function.

## Scope

### In Scope
- 4 JPA entities: `DriveProcess`, `DriveDocument`, `DriveVersion`, `DriveDocumentLog`
- 4 Spring Data repositories: one per entity, extending `JpaRepository<Entity, Long>`
- 2 enums: `DocumentStatus` (draft/under_review/published/archived), `DocumentAction` (UPLOAD/REVIEW_REQUEST/PUBLISH/REJECT/VERSION_UPDATE/OBSOLETE)
- Custom `AttributeConverter` for `DocumentStatus` (DB stores lowercase, Java uses UPPERCASE constants)
- `@Enumerated(EnumType.STRING)` for `DocumentAction` (DB and Java both UPPERCASE)
- Bidirectional `@OneToMany`/`@ManyToOne` relationships with `LAZY` fetch
- `@JsonIgnore` on all `@OneToMany` collections (serialization safety for Phase 3)
- Soft delete via `deletedAt` field + derived queries (`findAllByDeletedAtIsNull`)
- `@PrePersist`/`@PreUpdate` timestamp management; `DriveDocumentLog` gets `@PrePersist` only
- `user_id` as plain `Long` (no `@ManyToOne` — FK to SEC_Users deferred to Phase 8)

### Out of Scope
- Controllers (Phase 3)
- Services (Phase 4)
- DTOs / response mappers (Phase 9)
- User entity or FK constraints on `user_id`
- `group_type`, `document_type`, `main_category` as enums (remain free-form `String`)

## Capabilities

### New Capabilities
- `domain-model`: JPA entities, repositories, enums, and converters for the four DRIVE tables

### Modified Capabilities
- `drive-database-schema`: entity mapping adds runtime validation against existing V001 schema (no schema change, but entities now enforce column-level contracts)

## Approach

Faithful layered JPA model matching V001 exactly. Key decisions:

| Decision | Rationale |
|----------|-----------|
| `AttributeConverter` for `DocumentStatus` | DB stores lowercase (`draft`), Java enum is `DRAFT` — `@Enumerated(STRING)` would write wrong data |
| No JPA `cascade` | Rely on DB-level `ON DELETE CASCADE`; avoids double-delete semantics with `ddl-auto=validate` |
| `@Getter/@Setter` not `@Data` | Prevents `toString`/`equals`/`hashCode` recursion on bidirectional collections |
| `@JsonIgnore` on collections | Prevents lazy-init and infinite recursion when Phase 3 serializes entities directly |
| `findAllByOrderByNameAsc` on `DriveProcessRepository` | Unblocks the existing `process-api` spec (controller still deferred to Phase 3) |

Package layout: `model/` (entities + `model/enums/`), `repository/`.

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/.../model/` | New | 4 entity classes + 2 enums + 1 converter |
| `src/main/java/.../repository/` | New | 4 repository interfaces |
| `src/test/java/.../model/` | New | Entity mapping + converter tests |
| `src/test/java/.../repository/` | New | Repository integration tests (`@DataJpaTest`) |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| Entity/schema mismatch fails `ddl-auto=validate` | Medium | TDD with `@DataJpaTest` against live PostgreSQL before each commit |
| `DocumentStatus` converter read/write asymmetry | Low | Roundtrip test: persist `DRAFT` → reload → assert `DRAFT` |
| Lazy-init exceptions in Phase 3 if `@JsonIgnore` skipped | Medium | Mark collections `@JsonIgnore` now, verify with serialization test |
| `process-api` controller gap (spec exists, no entity yet) | Low | Phase 2 delivers entity+repo; controller explicitly deferred to Phase 3 |

## Rollback Plan

Delete the `model/`, `model/enums/`, and `repository/` packages. No schema changes — V001 migration is untouched. Application returns to Phase 1 state (compiles, starts, but has no domain persistence).

## Dependencies

- Phase 1 commits 1–4 (pom, compose, properties, V001 migration, SecurityConfig)
- Live PostgreSQL via `compose.yaml` for integration tests

## Success Criteria

- [ ] All 4 entities compile and map correctly against V001 (`ddl-auto=validate` passes)
- [ ] `DocumentStatus` converter roundtrips lowercase ↔ enum correctly
- [ ] `DocumentAction` persists as UPPERCASE strings
- [ ] All 4 repositories pass `@DataJpaTest` integration tests
- [ ] `DriveProcessRepository.findAllByOrderByNameAsc()` returns ordered results
- [ ] `DriveDocumentRepository.findAllByDeletedAtIsNull()` excludes soft-deleted rows
- [ ] Bidirectional collections are `@JsonIgnore`-annotated
- [ ] `./mvnw spotless:apply` passes (Google Java Format)

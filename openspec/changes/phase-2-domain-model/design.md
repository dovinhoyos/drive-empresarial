# Design: Phase 2 — Domain Model

## Technical Approach

Faithful JPA entity layer mapping V001 schema exactly. Four entities (`DriveProcess`, `DriveDocument`, `DriveVersion`, `DriveDocumentLog`) in `model/`, four Spring Data repositories in `repository/`, two enums in `model/enums/`. Bidirectional lazy relationships, DB-owned cascade, custom `AttributeConverter` for `DocumentStatus`. No schema changes — `ddl-auto=validate` enforces entity-schema alignment at startup.

## Architecture Decisions

| ADR | Choice | Alternatives | Rationale |
|-----|--------|--------------|-----------|
| ADR-005: AttributeConverter for DocumentStatus | Custom `@Converter` mapping `DRAFT ↔ draft`, `UNDER_REVIEW ↔ under_review` | `@Enumerated(STRING)` with lowercase constants (non-idiomatic); same constants uppercase (writes wrong data to DB) | DB stores lowercase (`draft`), Java enum convention is UPPERCASE (`DRAFT`). `@Enumerated(STRING)` would persist `DRAFT` ≠ `draft`, breaking semantic consumers. AttributeConverter adds ~20 lines but is type-safe and idiomatic. |
| ADR-006: Plain Long user_id | `Long userId` field, no `@ManyToOne` | Create stub `User` entity; FK constraint now | `SEC_Users` lives in a separate module. FK now requires stub table or cross-schema reference (fragile). Phase 8 adds FK after user module is stable. Mitigated by application-level validation. |
| ADR-007: DB-owned cascade | No `cascade` on JPA `@OneToMany`, `orphanRemoval = false` | `cascade = CascadeType.ALL`; `cascade = CascadeType.REMOVE` | V001 defines `ON DELETE CASCADE` on all FKs. JPA cascade + DB cascade = double-delete semantics with `ddl-auto=validate`. Children saved via their own repositories (explicit, testable). |
| ADR-008: @Getter/@Setter over @Data | `@Getter @Setter @NoArgsConstructor` per entity | `@Data` (includes toString/equals/hashCode) | `@Data` generates `toString`/`equals`/`hashCode` that recurse on bidirectional `@OneToMany` collections → `StackOverflowError` or silent performance issues. `@Getter/@Setter` gives field access without side effects. |

## Entity Relationship Diagram

```
┌──────────────────────┐
│   drive_processes    │
│──────────────────────│
│ id            PK     │
│ name                 │
│ prefix        UQ     │
│ group_type           │
│ created_at           │
│ updated_at           │
└──────┬───────────────┘
       │ 1:N
       ▼
┌──────────────────────────────────┐
│       drive_documents            │
│──────────────────────────────────│
│ id                        PK     │
│ title                          │
│ document_number           UQ   │
│ drive_process_id    FK → drive_processes
│ main_category                │
│ group_type                 │
│ document_type              │
│ status (DRAFT default)     │
│ rejection_notes             │
│ current_version (1.0)      │
│ created_at                   │
│ updated_at                   │
│ deleted_at (soft delete)  │
└──────┬───────────────┬──────────┘
       │ 1:N           │ 1:N
       ▼               ▼
┌──────────────────┐  ┌──────────────────────────┐
│  drive_versions  │  │  drive_document_logs     │
│──────────────────│  │──────────────────────────│
│ id          PK   │  │ id                 PK    │
│ drive_document_id│  │ drive_document_id  FK    │
│ version_number  │  │ user_id            (Long)│
│ s3_key          │  │ action (UPLOAD, etc.)    │
│ change_summary  │  │ version_number           │
│ user_id (Long)  │  │ notes                    │
│ created_at      │  │ created_at               │
│ updated_at      │  └──────────────────────────┘
└─────────────────┘
```

**No updated_at** on `drive_document_logs` (schema has only `created_at`).

## Package Structure

```
com/dovindev/driveempresarial/
├── config/SecurityConfig.java          (existing)
├── security/JwtAuthenticationFilter.java (existing)
├── model/
│   ├── DriveProcess.java
│   ├── DriveDocument.java
│   ├── DriveVersion.java
│   ├── DriveDocumentLog.java
│   └── enums/
│       ├── DocumentStatus.java
│       └── DocumentAction.java
├── repository/
│   ├── DriveProcessRepository.java
│   ├── DriveDocumentRepository.java
│   ├── DriveVersionRepository.java
│   └── DriveDocumentLogRepository.java
```

### Repository Derived Methods

| Repository | Method | Purpose |
|------------|--------|---------|
| `DriveProcessRepository` | `findAllByOrderByNameAsc()` | process-api: ordered list |
| `DriveDocumentRepository` | `findAllByDeletedAtIsNull()` | soft-delete filter |
| `DriveDocumentRepository` | `findByProcessIdAndDeletedAtIsNull(Long)` | docs by process |
| `DriveVersionRepository` | `findByDocumentIdOrderByCreatedAtDesc(Long)` | latest version (Phase 5) |
| `DriveDocumentLogRepository` | `findByDocumentIdOrderByCreatedAtDesc(Long)` | audit trail |

## Lombok Strategy

Per entity: `@Getter @Setter @NoArgsConstructor`. Explicit `@Column(name = "...")` for compound names (`driveProcessId → drive_process_id`, `createdAt → created_at`). **No `@Data`** — avoids `toString`/`equals`/`hashCode` recursion on bidirectional collections. If `equals`/`hashCode` needed later, use `@EqualsAndHashCode(onlyExplicitlyIncluded = true)` on `id`.

## Timestamps & Defaults

- `DriveProcess`, `DriveDocument`, `DriveVersion`: `@PrePersist` sets `createdAt`, `@PreUpdate` sets `updatedAt`
- `DriveDocumentLog`: `@PrePersist` only (no `updated_at` column)
- `DriveDocument`: `status` defaults to `DRAFT` in entity constructor; `currentVersion` defaults to `"1.0"`

## Soft Delete

Application-level filtering only. `deletedAt` as plain `LocalDateTime` field. No `@SQLDelete`/`@SQLRestriction`. Repos use derived queries: `findAllByDeletedAtIsNull()`.

## Serialization Safety

All `@OneToMany` collections annotated with `@JsonIgnore` to prevent lazy-init and infinite recursion when Phase 3 serializes entities directly (no DTO layer until Phase 9).

## Enum Storage

| Enum | Strategy | DB Values | Java Constants |
|------|----------|-----------|----------------|
| `DocumentStatus` | `@Converter(autoApply = true)` | `draft, under_review, published, archived` | `DRAFT, UNDER_REVIEW, PUBLISHED, ARCHIVED` |
| `DocumentAction` | `@Enumerated(EnumType.STRING)` | `UPLOAD, REVIEW_REQUEST, PUBLISH, REJECT, VERSION_UPDATE, OBSOLETE` | Same (UPPERCASE matches DB) |

## Testing Strategy

| Layer | What to Test | Approach |
|-------|-------------|----------|
| Converter | `DocumentStatus` roundtrip (DRAFT → persist → reload → DRAFT) | Unit test with `@ExtendWith(MockitoExtension.class)` or `@DataJpaTest` |
| Entity mapping | All 4 entities validate against V001 | `@DataJpaTest` + live PostgreSQL (`@ActiveProfiles("test")`) |
| Repository | Derived queries return correct results | `@DataJpaTest` with `TestEntityManager`, seed data via `@BeforeEach` |
| Converter integration | Auto-apply converter reads/writes correctly | `@DataJpaTest` — persist entity with `UNDER_REVIEW`, reload, assert enum value |

## Threat Matrix

N/A — no routing, shell, subprocess, VCS/PR automation, executable-file classification, or process-integration boundary.

## Migration / Rollout

No migration required. V001 is untouched. Entity layer is additive — application compiles and starts without it.

## Open Questions

- None — schema, enums, and relationships fully resolved from V001.

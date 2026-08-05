# Delta for DRIVE Database Schema

## ADDED Requirements

### Requirement: JPA Entity Mapping

The system MUST provide four JPA entities (`DriveProcess`, `DriveDocument`, `DriveVersion`, `DriveDocumentLog`) mapping to the V001 tables. Each entity MUST use `@Entity`, `@Table(name = "...")`, `@Id`, `@GeneratedValue(strategy = GenerationType.IDENTITY)`, and explicit `@Column(name = "...")` for compound column names.

#### Scenario: Entity compilation and validation

- GIVEN the four entity classes exist with correct annotations
- WHEN `ddl-auto=validate` runs at application startup
- THEN Hibernate validates all entity mappings against V001 schema without errors

#### Scenario: Compound column mapping

- GIVEN `DriveDocument` has field `driveProcessId`
- WHEN the entity is mapped to the database
- THEN the column name is `drive_process_id` via `@Column(name = "drive_process_id")`

### Requirement: DocumentStatus AttributeConverter

The system MUST provide a custom `AttributeConverter<DocumentStatus, String>` that converts between the Java enum (`DRAFT`, `UNDER_REVIEW`, `PUBLISHED`, `ARCHIVED`) and the lowercase DB values (`draft`, `under_review`, `published`, `archived`).

#### Scenario: Persist DocumentStatus as lowercase

- GIVEN a `DriveDocument` with status `DRAFT`
- WHEN the entity is persisted via JPA
- THEN the `status` column stores the value `draft` (lowercase)

#### Scenario: Read DocumentStatus from DB

- GIVEN a row in `drive_documents` with `status = 'published'`
- WHEN the entity is loaded via JPA
- THEN the `status` field is `DocumentStatus.PUBLISHED`

#### Scenario: Roundtrip converter correctness

- GIVEN a `DriveDocument` with status `UNDER_REVIEW`
- WHEN the entity is persisted and then reloaded
- THEN the status field equals `DocumentStatus.UNDER_REVIEW`

### Requirement: DocumentAction Enum Storage

The system MUST persist `DocumentAction` using `@Enumerated(EnumType.STRING)` — the DB values (`UPLOAD`, `REVIEW_REQUEST`, `PUBLISH`, `REJECT`, `VERSION_UPDATE`, `OBSOLETE`) match the Java enum constant names.

#### Scenario: Persist DocumentAction as string

- GIVEN a `DriveDocumentLog` with action `UPLOAD`
- WHEN the entity is persisted via JPA
- THEN the `action` column stores the value `UPLOAD`

### Requirement: Bidirectional Relationships with Lazy Fetch

The system MUST define bidirectional `@OneToMany`/`@ManyToOne` relationships: DriveProcess ↔ DriveDocument, DriveDocument ↔ DriveVersion, DriveDocument ↔ DriveDocumentLog. The `@ManyToOne` side MUST use `fetch = FetchType.LAZY`. The `@OneToMany` side MUST use `@JsonIgnore` to prevent serialization recursion.

#### Scenario: Lazy loading on ManyToOne

- GIVEN a `DriveDocument` loaded via repository
- WHEN the `process` field is accessed outside a transaction
- THEN a `LazyInitializationException` is thrown (or no query if not accessed)

#### Scenario: JsonIgnore on collections

- GIVEN a `DriveProcess` with documents
- WHEN serialized to JSON
- THEN the `documents` field is excluded from the output

### Requirement: Soft Delete with deletedAt

The system MUST support soft delete on `DriveDocument` via a `deletedAt` field (`LocalDateTime`, nullable). The repository MUST provide `findAllByDeletedAtIsNull()` to query only active records. No `@SQLDelete` or `@SQLRestriction` is used.

#### Scenario: Exclude soft-deleted documents

- GIVEN documents with `deleted_at = NULL` and `deleted_at = '2026-01-01'`
- WHEN `findAllByDeletedAtIsNull()` is called
- THEN only the documents with `deleted_at IS NULL` are returned

### Requirement: Timestamp Lifecycle Management

The system MUST set `createdAt` and `updatedAt` via `@PrePersist`/`@PreUpdate` on all entities except `DriveDocumentLog`. `DriveDocumentLog` MUST use `@PrePersist` only (no `updated_at` column in V001).

#### Scenario: Timestamps set on persist

- GIVEN a new `DriveProcess` entity
- WHEN the entity is persisted
- THEN both `createdAt` and `updatedAt` are set to the current timestamp

#### Scenario: DriveDocumentLog has no updatedAt

- GIVEN a `DriveDocumentLog` entity
- WHEN the entity is persisted
- THEN `createdAt` is set
- AND no `updatedAt` field exists on the entity

### Requirement: No JPA Cascade

The system MUST NOT declare JPA `cascade` or `orphanRemoval` on any `@OneToMany` collection. Children are deleted via DB-level `ON DELETE CASCADE` from V001.

#### Scenario: Orphan removal disabled

- GIVEN a `DriveProcess` with two documents
- WHEN one document is removed from the `documents` list in memory
- THEN the removed document is NOT deleted from the database (no orphanRemoval)

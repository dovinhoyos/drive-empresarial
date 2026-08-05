# DRIVE Database Schema Specification

## Purpose

Define the PostgreSQL schema for all DRIVE domain tables via Flyway migration V001, and the JPA entity layer that maps to it. Tables: `drive_processes`, `drive_documents`, `drive_versions`, `drive_document_logs`.

## Requirements

### Requirement: drive_processes Table

The system MUST create a `drive_processes` table with columns: `id` (BIGSERIAL PK), `name` (VARCHAR 255 NOT NULL), `prefix` (VARCHAR 5 NOT NULL), `group_type` (VARCHAR 50), `created_at` (TIMESTAMP), `updated_at` (TIMESTAMP).

#### Scenario: Create process

- GIVEN the V001 migration has run
- WHEN a new row is inserted with name, prefix, and group_type
- THEN the row is persisted with auto-generated id and timestamps

#### Scenario: Unique prefix constraint

- GIVEN a process with prefix 'CAL' exists
- WHEN another process with prefix 'CAL' is inserted
- THEN the database raises a unique constraint violation

### Requirement: drive_documents Table

The system MUST create a `drive_documents` table with columns: `id` (BIGSERIAL PK), `title` (VARCHAR 255 NOT NULL), `document_number` (VARCHAR 255 UNIQUE, nullable), `drive_process_id` (FK → drive_processes ON DELETE CASCADE), `main_category` (VARCHAR 100), `group_type` (VARCHAR 50), `document_type` (VARCHAR 10), `status` (VARCHAR 20 DEFAULT 'draft'), `rejection_notes` (TEXT), `current_version` (VARCHAR 10 DEFAULT '1.0'), `created_at` (TIMESTAMP), `updated_at` (TIMESTAMP), `deleted_at` (TIMESTAMP).

#### Scenario: Create document in draft state

- GIVEN the V001 migration has run
- WHEN a new document is inserted with title and drive_process_id
- THEN status defaults to 'draft'
- AND current_version defaults to '1.0'
- AND document_number is NULL

#### Scenario: Soft delete

- GIVEN a document with deleted_at set
- WHEN querying active documents
- THEN the document is excluded from results (application-level filtering)

#### Scenario: Cascade delete from process

- GIVEN a process with associated documents
- WHEN the process is deleted
- THEN all associated documents are cascade-deleted

### Requirement: drive_versions Table

The system MUST create a `drive_versions` table with columns: `id` (BIGSERIAL PK), `drive_document_id` (FK → drive_documents ON DELETE CASCADE), `version_number` (VARCHAR 10), `s3_key` (VARCHAR 500), `change_summary` (TEXT), `user_id` (BIGINT), `created_at` (TIMESTAMP), `updated_at` (TIMESTAMP).

#### Scenario: Create version

- GIVEN a document exists
- WHEN a new version is inserted with version_number, s3_key, and user_id
- THEN the version is linked to the document
- AND deleting the document cascade-deletes all versions

### Requirement: drive_document_logs Table

The system MUST create a `drive_document_logs` table with columns: `id` (BIGSERIAL PK), `drive_document_id` (FK → drive_documents ON DELETE CASCADE), `user_id` (BIGINT), `action` (VARCHAR 50), `version_number` (VARCHAR 10), `notes` (TEXT), `created_at` (TIMESTAMP).

#### Scenario: Create audit log entry

- GIVEN a document exists
- WHEN a log entry is inserted with action 'PUBLISH' and user_id
- THEN the entry is persisted with created_at timestamp
- AND deleting the document cascade-deletes all log entries

### Requirement: Foreign Key Constraints

The system MUST enforce foreign key relationships: drive_documents → drive_processes (CASCADE), drive_versions → drive_documents (CASCADE), drive_document_logs → drive_documents (CASCADE).

#### Scenario: Referential integrity

- GIVEN a document linked to process id=1
- WHEN attempting to delete the process with id=1
- THEN the database either cascade-deletes the document or blocks deletion depending on FK type

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

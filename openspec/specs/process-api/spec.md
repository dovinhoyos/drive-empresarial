# Process API Specification

## Purpose

Provide a read-only API endpoint to list all DRIVE processes. This serves as the smoke-test endpoint to verify the full stack (controller → repository → database) is working.

## Requirements

### Requirement: GET /api/drive/processes

The system MUST return a JSON array of all drive_processes rows, ordered by name ascending.

#### Scenario: List processes with data

- GIVEN 3 processes exist in the database (Calidad, Gestión, Planeación)
- WHEN GET /api/drive/processes is called
- THEN the response is HTTP 200
- AND the body contains a JSON array of 3 process objects
- AND each object has id, name, prefix, groupType, createdAt, updatedAt

#### Scenario: List processes when empty

- GIVEN no processes exist in the database
- WHEN GET /api/drive/processes is called
- THEN the response is HTTP 200
- AND the body contains an empty JSON array []

#### Scenario: JSON field naming convention

- GIVEN a process with name='Calidad' and prefix='CAL'
- WHEN GET /api/drive/processes returns the process
- THEN the JSON keys use camelCase (groupType, createdAt, updatedAt)
- AND the Java field names use camelCase (matching Lombok @JsonProperty defaults)

### Requirement: DriveProcess JPA Entity

The system MUST provide a `DriveProcess` entity mapping to `drive_processes` with fields: `id` (Long, auto-generated), `name` (String), `prefix` (String), `groupType` (String), `createdAt` (LocalDateTime), `updatedAt` (LocalDateTime). Field `groupType` maps to column `group_type` via `@Column(name = "group_type")`.

#### Scenario: Entity maps to drive_processes

- GIVEN the `DriveProcess` entity class with `@Table(name = "drive_processes")`
- WHEN Hibernate validates the schema
- THEN all columns match V001 types: `id` (BIGSERIAL), `name` (VARCHAR 255), `prefix` (VARCHAR 5), `group_type` (VARCHAR 50)

#### Scenario: Process persist with timestamps

- GIVEN a new `DriveProcess` with name='Calidad', prefix='CAL', groupType='MISIONAL'
- WHEN the entity is persisted via repository
- THEN `createdAt` and `updatedAt` are automatically set
- AND `id` is auto-generated

### Requirement: DriveProcess Repository

The system MUST provide a `DriveProcessRepository` extending `JpaRepository<DriveProcess, Long>` with a derived method `findAllByOrderByNameAsc()`.

#### Scenario: Ordered query by name

- GIVEN processes 'Planeación', 'Calidad', 'Gestión' in the database
- WHEN `findAllByOrderByNameAsc()` is called
- THEN the result list is ordered: Calidad, Gestión, Planeación

#### Scenario: Empty result set

- GIVEN no processes exist in the database
- WHEN `findAllByOrderByNameAsc()` is called
- THEN an empty list is returned

### Requirement: DriveDocument Repository

The system MUST provide a `DriveDocumentRepository` extending `JpaRepository<DriveDocument, Long>` with `findAllByDeletedAtIsNull()` and `findByProcessIdAndDeletedAtIsNull(Long processId)`.

#### Scenario: Query active documents

- GIVEN documents with `deleted_at = NULL` and `deleted_at = '2026-01-01'`
- WHEN `findAllByDeletedAtIsNull()` is called
- THEN only documents with `deleted_at IS NULL` are returned

#### Scenario: Query active documents by process

- GIVEN process id=1 with active and soft-deleted documents
- WHEN `findByProcessIdAndDeletedAtIsNull(1L)` is called
- THEN only active documents for that process are returned

### Requirement: DriveVersion Repository

The system MUST provide a `DriveVersionRepository` extending `JpaRepository<DriveVersion, Long>` with `findByDocumentIdOrderByCreatedAtDesc(Long documentId)`.

#### Scenario: Versions ordered by creation date

- GIVEN document id=1 with versions v1.0 (2026-01-01) and v1.1 (2026-01-15)
- WHEN `findByDocumentIdOrderByCreatedAtDesc(1L)` is called
- THEN versions are returned in descending order: v1.1, v1.0

### Requirement: DriveDocumentLog Repository

The system MUST provide a `DriveDocumentLogRepository` extending `JpaRepository<DriveDocumentLog, Long>` with `findByDocumentIdOrderByCreatedAtDesc(Long documentId)`.

#### Scenario: Logs ordered by creation date

- GIVEN document id=1 with two log entries
- WHEN `findByDocumentIdOrderByCreatedAtDesc(1L)` is called
- THEN log entries are returned in descending chronological order

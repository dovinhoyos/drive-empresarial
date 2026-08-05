# DRIVE Database Schema Specification

## Purpose

Define the PostgreSQL schema for all DRIVE domain tables via Flyway migration V001. Tables: `drive_processes`, `drive_documents`, `drive_versions`, `drive_document_logs`.

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

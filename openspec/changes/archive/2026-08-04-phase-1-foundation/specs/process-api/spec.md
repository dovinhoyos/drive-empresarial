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

### Requirement: DriveProcess Entity Mapping

The system MUST map the `drive_processes` table to a JPA entity `DriveProcess` with proper column annotations.

#### Scenario: Entity annotations

- GIVEN the DriveProcess entity class
- WHEN Hibernate scans the entity
- THEN it maps to table 'drive_processes'
- AND all columns map with correct types and constraints

### Requirement: DriveProcess Repository

The system MUST provide a `DriveProcessRepository` extending `JpaRepository<DriveProcess, Long>`.

#### Scenario: Find all processes

- GIVEN multiple processes in the database
- WHEN repository.findAll() is called
- THEN all processes are returned in natural order

# Tasks: Phase 1 — Foundation

## Review Workload Forecast

- **Estimated changed lines**: ~350 (well under 400-line budget)
- **Decision needed before apply**: No
- **Chained PRs recommended**: No
- **400-line budget risk**: Low
- **Delivery strategy**: single-pr (fits in one PR)

## Tasks

### 1. Project Setup
- [x] 1.1 Verify Spring Boot project structure (pom.xml, main class, application.properties)
- [x] 1.2 Configure Docker Compose (PostgreSQL 16, MinIO)
- [x] 1.3 Verify `./mvnw compile` succeeds

### 2. Flyway Migration V001
- [x] 2.1 Create `src/main/resources/db/migration/V001__create_drive_tables.sql`
  - Create `drive_processes` table (id, name, prefix, group_type, created_at, updated_at)
  - Create `drive_documents` table (all columns per schema, FK to drive_processes CASCADE)
  - Create `drive_versions` table (all columns per schema, FK to drive_documents CASCADE)
  - Create `drive_document_logs` table (all columns per schema, FK to drive_documents CASCADE)
  - NO FKs to SEC_Users (deferred to Phase 8)
- [x] 2.2 Verify migration applies on app startup

### 3. SecurityConfig + JWT Filter
- [x] 3.1 Create `src/main/java/com/dovindev/driveempresarial/security/JwtAuthenticationFilter.java`
  - Extend OncePerRequestFilter
  - Extract Bearer token from Authorization header
  - Validate JWT (fail on default secret)
  - Set SecurityContext on valid token
  - Log warning on invalid/missing token, proceed without auth
- [x] 3.2 Create `src/main/java/com/dovindev/driveempresarial/config/SecurityConfig.java`
  - Permit all requests to `/api/drive/**`
  - Stateless session management
  - CORS configuration with wildcard guard
  - Register JwtAuthenticationFilter
- [x] 3.3 Add JWT secret config to application.properties with env var override

### 4. Integration Tests
- [x] 4.1 Create `MigrationV001Test.java` — verifies Flyway migration and 4 tables
- [x] 4.2 Create `SecurityConfigTest.java` — verifies permit-all (placeholder, real test in Phase 2)
- [x] 4.3 Create `JwtAuthenticationFilterTest.java` — unit tests for token handling
- [x] 4.4 Verify all tests pass: `./mvnw test` (16/16 passing)

## Commit Strategy

| Commit | Description | Files |
|--------|-------------|-------|
| 1 | feat: initial project setup | pom.xml, compose.yaml, application.properties, main class |
| 2 | feat: add database config and Flyway | V001__create_drive_tables.sql |
| 3 | feat: add SecurityConfig and JWT filter | SecurityConfig.java, JwtAuthenticationFilter.java |
| 4 | test: add integration and unit tests | MigrationV001Test, SecurityConfigTest, JwtAuthenticationFilterTest |

## Deferred to Phase 2

- DriveProcess entity, repository, controller
- DriveProcessControllerTest (real endpoint test)
- SecurityConfigTest rewrite with controller dummy
- CHECK constraints for status and group_type

# Proposal: Phase 1 — Foundation

## Intent

Establish the working foundation for the DRIVE module migration from Laravel to Spring Boot. Without git history, database schema, security skeleton, and a smoke-test endpoint, no further phases can proceed or be verified.

## Scope

### In Scope
1. **Git init + initial commit** — version-control existing setup (pom.xml, compose.yaml, application.properties, main class)
2. **SecurityConfig** — Spring Security skeleton with JWT filter (full RBAC deferred to Phase 8)
3. **V001 Flyway migration** — create `drive_processes`, `drive_documents`, `drive_versions`, `drive_document_logs` tables
4. **GET /api/drive/processes** — smoke-test endpoint (entity + repository + controller)

### Out of Scope
- Full RBAC / policy checks (Phase 8)
- S3/MinIO integration (Phase 6)
- Document lifecycle endpoints (Phase 4)
- Audit trail logging (Phase 7)

## Capabilities

### New Capabilities
- `security-skeleton`: Spring Security config + JWT filter skeleton (permit-all for /api/drive/** initially)
- `drive-database-schema`: Flyway V001 migration creating all 4 DRIVE tables with FKs
- `process-api`: GET /api/drive/processes endpoint (entity, repository, controller)

### Modified Capabilities
None — this is the initial foundation.

## Approach

1. `git init` + commit existing files
2. Create `SecurityConfig.java` (permit all, CORS, stateless) + `JwtAuthenticationFilter.java` (extract/validate JWT, set SecurityContext)
3. Write `V001__create_drive_tables.sql` with all 4 tables, proper FKs, indexes
4. Create `DriveProcess` entity, `DriveProcessRepository`, `DriveProcessController` with GET /api/drive/processes
5. TDD: write integration test for GET /processes before implementing

## Affected Areas

| Area | Impact | Description |
|------|--------|-------------|
| `src/main/java/.../config/` | New | SecurityConfig.java |
| `src/main/java/.../security/` | New | JwtAuthenticationFilter.java |
| `src/main/resources/db/migration/` | New | V001__create_drive_tables.sql |
| `src/main/java/.../model/` | New | DriveProcess.java |
| `src/main/java/.../repository/` | New | DriveProcessRepository.java |
| `src/main/java/.../controller/` | New | DriveProcessController.java |
| `src/test/java/.../` | New | Integration tests |

## Risks

| Risk | Likelihood | Mitigation |
|------|------------|------------|
| SEC_Users FKs fail (table doesn't exist yet) | High | Use deferred FK constraints or omit user FKs in V001, add in Phase 8 |
| JWT secret not configured | Low | Use placeholder in application.properties, override in production |

## Rollback Plan

- Revert git commits (each commit is atomic)
- Flyway: `flyway undo` or drop tables manually
- No production data at risk (greenfield project)

## Dependencies

- PostgreSQL running (via compose.yaml)
- No dependency on SEC_Users table (FKs deferred)

## Success Criteria

- [ ] Git repo initialized with clean history
- [ ] `./mvnw test` passes (at minimum: context loads, Flyway migrates)
- [ ] GET /api/drive/processes returns 200 with empty list
- [ ] SecurityConfig allows unauthenticated access to /api/drive/** (temporarily)

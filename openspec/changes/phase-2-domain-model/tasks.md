# Tasks: Phase 2 — Domain Model

## Review Workload Forecast

Decision needed before apply: Yes
Chained PRs recommended: Yes
Chain strategy: pending
400-line budget risk: High

Estimated changed lines: ~950 (main ~460 + tests ~490). Exceeds 400-line budget → chained PRs.

## Suggested Work Units

| Unit | Goal | PR | Focused test command | Runtime harness | Rollback boundary |
|------|------|----|----------------------|-----------------|-------------------|
| 1 | Enums + converter + DriveProcess | PR 1 | `./mvnw test -Dtest=DocumentStatusConverterTest,DriveProcessRepositoryTest` | `docker compose up -d`; live PG required | delete `model/enums/`, `DriveProcess.java`, repo |
| 2 | DriveDocument entity + repo | PR 2 | `./mvnw test -Dtest=DriveDocumentRepositoryTest` | `docker compose up -d`; live PG required | delete `DriveDocument.java`, repo, test |
| 3 | DriveVersion + DriveDocumentLog + V002 + SecurityConfigTest | PR 3 | `./mvnw test` | `docker compose up -d`; live PG required | delete entities/repos, revert V002 |

Feature Branch Chain bases: PR 1 → tracker branch; PR 2 → PR 1 branch; PR 3 → PR 2 branch.

## Commit Strategy (MIGRATION_PLAN)

| Commit | Description | Files |
|--------|-------------|-------|
| 5 | feat: add DriveProcess entity | DriveProcess.java, DriveProcessRepository.java |
| 6 | feat: add DriveDocument entity | DriveDocument.java, DriveDocumentRepository.java |
| 7 | feat: add DriveVersion entity | DriveVersion.java, DriveVersionRepository.java |
| 8 | feat: add DriveDocumentLog entity | DriveDocumentLog.java, DriveDocumentLogRepository.java |
| 9 | feat: add enums | DocumentStatus.java, DocumentAction.java |

Order note: commits 6/8 depend on 9 — implement enums+converter first; PR grouping above honors this.

## TDD Cycle Notes

- strict_tdd=true: each task is RED (failing test) → GREEN (implement) → REFACTOR (spotless).
- Integration tests need live PostgreSQL: `docker compose up -d` before `./mvnw test`.
- Run `./mvnw spotless:apply` after each GREEN.

## Phase 1: Enums + Converter (foundation)

- [x] 1.1 RED: `model/enums/DocumentStatusConverterTest.java` — toDb(DRAFT)=\"draft\", fromDb(\"published\")=PUBLISHED, roundtrip
- [x] 1.2 GREEN: `model/enums/DocumentStatus.java` — DRAFT, UNDER_REVIEW, PUBLISHED, ARCHIVED
- [x] 1.3 GREEN: `model/enums/DocumentAction.java` — UPLOAD, REVIEW_REQUEST, PUBLISH, REJECT, VERSION_UPDATE, OBSOLETE
- [x] 1.4 GREEN: `model/enums/DocumentStatusConverter.java` — `@Converter(autoApply=true)`, enum↔lowercase
- [x] 1.5 REFACTOR: `./mvnw spotless:apply`; converter test green

## Phase 2: DriveProcess (commit 5)

- [ ] 2.1 RED: `DriveProcessRepositoryTest.java` (`@DataJpaTest`, `@ActiveProfiles(\"test\")`) — order-by-name, empty set, timestamps set
- [x] 2.2 GREEN: `model/DriveProcess.java` — `@Table(\"drive_processes\")`, IDENTITY id, name/prefix/groupType, `@OneToMany(mappedBy=\"process\")` `@JsonIgnore` documents, no cascade, `@PrePersist/@PreUpdate`, `@Getter @Setter @NoArgsConstructor`
- [x] 2.3 GREEN: `repository/DriveProcessRepository.java` — `JpaRepository<DriveProcess, Long>` + `findAllByOrderByNameAsc()`
- [ ] 2.4 REFACTOR: spotless; repo test green

## Phase 3: DriveDocument (commit 6)

- [ ] 3.1 RED: `DriveDocumentRepositoryTest.java` — `findAllByDeletedAtIsNull()` excludes deleted, `findByProcessIdAndDeletedAtIsNull(1L)`, status roundtrip (UNDER_REVIEW)
- [ ] 3.2 GREEN: `model/DriveDocument.java` — `@Table("drive_documents")`; `@ManyToOne(fetch=LAZY)` process + `@JoinColumn("drive_process_id")`; status defaults DRAFT; currentVersion "1.0"; deletedAt; `@JsonIgnore` versions/logs; `@PrePersist/@PreUpdate`
- [ ] 3.3 GREEN: `repository/DriveDocumentRepository.java` — both derived queries (processId traverses `process.id`)
- [ ] 3.4 RED+GREEN: serialization test (ObjectMapper) — DriveProcess JSON excludes `documents`

## Phase 4: DriveVersion (commit 7)

- [ ] 4.1 RED: `DriveVersionRepositoryTest.java` — `findByDocumentIdOrderByCreatedAtDesc` returns v1.1, v1.0
- [ ] 4.2 GREEN: `model/DriveVersion.java` — `@Table("drive_versions")`; `@ManyToOne(LAZY)` document; versionNumber, s3Key, changeSummary, userId plain Long; `@PrePersist/@PreUpdate`
- [ ] 4.3 GREEN: `repository/DriveVersionRepository.java` — `findByDocumentIdOrderByCreatedAtDesc(Long)`

## Phase 5: DriveDocumentLog (commit 8)

- [ ] 5.1 RED: `DriveDocumentLogRepositoryTest.java` — `findByDocumentIdOrderByCreatedAtDesc`; action persists UPPERCASE
- [ ] 5.2 GREEN: `model/DriveDocumentLog.java` — `@Table("drive_document_logs")`; action `@Enumerated(STRING)`; userId plain Long; `@PrePersist` only (no updatedAt)
- [ ] 5.3 GREEN: `repository/DriveDocumentLogRepository.java` — `findByDocumentIdOrderByCreatedAtDesc(Long)`

## Phase 6: Phase 1 Gaps + Verification

- [ ] 6.1 RED: rewrite `SecurityConfigTest.java` with test-scoped dummy controller mapping /api/drive/processes + /api/drive/documents; assert 200 permit-all + CORS, not 404
- [ ] 6.2 RED: add V002 assertions — status/group_type CHECK constraints exist (pg_constraint query)
- [ ] 6.3 GREEN: `V002__add_drive_check_constraints.sql` — CHECK status IN (draft,under_review,published,archived); CHECK group_type IN (ESTRATEGICO,MISIONAL,APOYO,EVALUACION)
- [ ] 6.4 Full run: `./mvnw test` green against live PG; `./mvnw spotless:apply` clean
- [ ] 6.5 Check off Phase 2 in MIGRATION_PLAN.md progress tracker

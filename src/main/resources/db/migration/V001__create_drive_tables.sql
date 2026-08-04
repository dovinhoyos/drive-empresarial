-- V001: Create DRIVE (Sistema Integral de Calidad) tables
-- Source of truth: Laravel DRIVE module schema

-- ============================================================
-- drive_processes
-- ============================================================
CREATE TABLE drive_processes (
    id              BIGSERIAL       PRIMARY KEY,
    name            VARCHAR(255)    NOT NULL,
    prefix          VARCHAR(5)      NOT NULL UNIQUE,
    group_type      VARCHAR(50),
    created_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE drive_processes IS 'Quality management processes (Calidad, Gestión, etc.)';
COMMENT ON COLUMN drive_processes.prefix IS 'Short code used in document numbering, e.g. CAL';
COMMENT ON COLUMN drive_processes.group_type IS 'ESTRATEGICO, MISIONAL, APOYO, EVALUACION';

-- ============================================================
-- drive_documents
-- ============================================================
CREATE TABLE drive_documents (
    id                  BIGSERIAL       PRIMARY KEY,
    title               VARCHAR(255)    NOT NULL,
    document_number     VARCHAR(255)    UNIQUE,
    drive_process_id      BIGINT          NOT NULL REFERENCES drive_processes(id) ON DELETE CASCADE,
    main_category       VARCHAR(100),
    group_type          VARCHAR(50),
    document_type       VARCHAR(10),
    status              VARCHAR(20)     NOT NULL DEFAULT 'draft',
    rejection_notes     TEXT,
    current_version     VARCHAR(10)     NOT NULL DEFAULT '1.0',
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    deleted_at          TIMESTAMP
);

COMMENT ON TABLE drive_documents IS 'DRIVE documents — draft through published lifecycle';
COMMENT ON COLUMN drive_documents.document_number IS 'Generated at publish time, e.g. PRC-CAL-001';
COMMENT ON COLUMN drive_documents.status IS 'draft, under_review, published, archived';
COMMENT ON COLUMN drive_documents.deleted_at IS 'Soft delete timestamp — NULL means active';

CREATE INDEX idx_drive_documents_process ON drive_documents(drive_process_id);
CREATE INDEX idx_drive_documents_status ON drive_documents(status);
CREATE INDEX idx_drive_documents_deleted ON drive_documents(deleted_at) WHERE deleted_at IS NULL;

-- ============================================================
-- drive_versions
-- ============================================================
CREATE TABLE drive_versions (
    id                  BIGSERIAL       PRIMARY KEY,
    drive_document_id     BIGINT          NOT NULL REFERENCES drive_documents(id) ON DELETE CASCADE,
    version_number      VARCHAR(10)     NOT NULL,
    s3_key              VARCHAR(500)    NOT NULL,
    change_summary      TEXT,
    user_id             BIGINT          NOT NULL,  -- FK to SEC_Users deferred to Phase 8
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE drive_versions IS 'Document versions with S3 file references';
COMMENT ON COLUMN drive_versions.version_number IS 'Semantic version: 1.0, 1.1, 2.0';
COMMENT ON COLUMN drive_versions.s3_key IS 'S3 object key for the version file';
COMMENT ON COLUMN drive_versions.user_id IS 'References SEC_Users(id_user) — FK added in Phase 8';

CREATE INDEX idx_drive_versions_document ON drive_versions(drive_document_id);

-- ============================================================
-- drive_document_logs
-- ============================================================
CREATE TABLE drive_document_logs (
    id                  BIGSERIAL       PRIMARY KEY,
    drive_document_id     BIGINT          NOT NULL REFERENCES drive_documents(id) ON DELETE CASCADE,
    user_id             BIGINT          NOT NULL,  -- FK to SEC_Users deferred to Phase 8
    action              VARCHAR(50)     NOT NULL,
    version_number      VARCHAR(10),
    notes               TEXT,
    created_at          TIMESTAMP       DEFAULT CURRENT_TIMESTAMP
);

COMMENT ON TABLE drive_document_logs IS 'Audit trail for document lifecycle events';
COMMENT ON COLUMN drive_document_logs.action IS 'UPLOAD, REVIEW_REQUEST, PUBLISH, REJECT, VERSION_UPDATE, OBSOLETE';
COMMENT ON COLUMN drive_document_logs.user_id IS 'References SEC_Users(id_user) — FK added in Phase 8';

CREATE INDEX idx_drive_document_logs_document ON drive_document_logs(drive_document_id);

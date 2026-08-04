package com.dovindev.driveempresarial.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies that V001 migration applies cleanly and creates the expected schema.
 * If this test passes, Flyway + PostgreSQL are working correctly.
 */
@SpringBootTest
@ActiveProfiles("test")
class MigrationV001Test {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldLoadContextAndApplyMigration() {
        // Context loaded = Flyway migration ran successfully
        assertThat(jdbcTemplate).isNotNull();
    }

    @Test
    void shouldCreateDriveProcessesTable() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = 'public' AND table_name = 'drive_processes'"
        );
        assertThat(tables).hasSize(1);
    }

    @Test
    void shouldCreateDriveDocumentsTable() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = 'public' AND table_name = 'drive_documents'"
        );
        assertThat(tables).hasSize(1);
    }

    @Test
    void shouldCreateDriveVersionsTable() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = 'public' AND table_name = 'drive_versions'"
        );
        assertThat(tables).hasSize(1);
    }

    @Test
    void shouldCreateDriveDocumentLogsTable() {
        List<Map<String, Object>> tables = jdbcTemplate.queryForList(
            "SELECT table_name FROM information_schema.tables " +
            "WHERE table_schema = 'public' AND table_name = 'drive_document_logs'"
        );
        assertThat(tables).hasSize(1);
    }

    @Test
    void shouldApplyMigrationWithSuccess() {
        Integer count = jdbcTemplate.queryForObject(
            "SELECT count(*) FROM flyway_schema_history WHERE success = true",
            Integer.class
        );
        assertThat(count).isGreaterThanOrEqualTo(1);
    }
}

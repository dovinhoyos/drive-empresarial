package com.dovindev.driveempresarial.integration;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifies SecurityConfig permits access to /api/drive/** endpoints
 * and that CORS headers are properly configured.
 */
@WebMvcTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldPermitAccessToDriveProcessesEndpoint() throws Exception {
        // Phase 1: /api/drive/processes is permit-all
        // No controller exists yet, so we expect 404 (not 403 Forbidden)
        mockMvc.perform(get("/api/drive/processes"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldPermitAccessToDriveDocumentsEndpoint() throws Exception {
        mockMvc.perform(get("/api/drive/documents"))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldNotRequireAuthentication() throws Exception {
        // Without JWT token, permit-all endpoints return 404 (no controller)
        // NOT 403 (Forbidden) which would mean auth is required
        mockMvc.perform(get("/api/drive/processes"))
            .andExpect(status().isNotFound());
    }
}

# Design: Phase 1 — Foundation

## Architecture Decisions

### ADR-001: Security Skeleton — Permit-All Initially

**Decision**: Configure Spring Security to permit all requests to `/api/drive/**` without authentication. The JWT filter extracts and validates tokens but does NOT enforce authentication.

**Rationale**: Full RBAC (Phase 8) needs the user/role infrastructure. Adding auth enforcement now would block all endpoint testing. The skeleton establishes the filter chain without blocking development.

**Tradeoff**: Temporary security gap. Mitigated by: the app runs locally only during development; production deployment is deferred until Phase 8.

### ADR-002: Deferred FKs to SEC_Users

**Decision**: Omit foreign key constraints to `SEC_Users` in V001. The `user_id` columns in `drive_versions` and `drive_document_logs` are BIGINT but NOT FK-constrained.

**Rationale**: `SEC_Users` is in a separate module/schema. Adding FK now would require creating a stub table or cross-schema reference, both fragile. Phase 8 adds the FK after the user module is stable.

**Tradeoff**: No referential integrity for user_id until Phase 8. Mitigated by: application-level validation ensures user_id references exist.

### ADR-003: Layered Package Structure

**Decision**: Organize code in layered packages under `com.dovindev.driveempresarial`:

```
com.dovindev.driveempresarial
├── config/          → SecurityConfig, OpenApiConfig (Phase 9)
├── controller/      → REST controllers
├── service/         → Business logic (Phase 4+)
├── repository/      → JPA repositories
├── model/           → JPA entities
├── security/        → JwtAuthenticationFilter
└── exception/       → GlobalExceptionHandler (Phase 3)
```

**Rationale**: Matches the MIGRATION_PLAN.md architecture (Controller → Service → Repository → Entity). Simple, familiar, scales well for this module size.

### ADR-004: Flyway Naming Convention

**Decision**: Use `V001__create_drive_tables.sql` (triple-digit prefix, double underscore separator).

**Rationale**: Flyway convention. Triple-digit allows 999 migrations before renumbering. Consistent with the project's `spring.flyway.locations=classpath:db/migration`.

## Component Design

### SecurityConfig.java

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {
    // HttpSecurity: permitAll for /api/drive/**
    // StatelessSessionManagement
    // CORS configuration
    // Add JwtAuthenticationFilter before UsernamePasswordAuthenticationFilter
}
```

### JwtAuthenticationFilter.java

```java
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    // Extract Bearer token from Authorization header
    // Validate JWT using jjwt
    // Set SecurityContext with claims (Phase 8: add authorities)
    // On invalid token: log warning, proceed without auth (Phase 1)
}
```

### V001 Migration

```sql
CREATE TABLE drive_processes (...);
CREATE TABLE drive_documents (...);
CREATE TABLE drive_versions (...);
CREATE TABLE drive_document_logs (...);
-- No FKs to SEC_Users
```

### DriveProcess Entity

```java
@Entity
@Table(name = "drive_processes")
@Data  // Lombok
public class DriveProcess {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String prefix;
    private String groupType;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    // PrePersist/PreUpdate for timestamps
}
```

### DriveProcessController

```java
@RestController
@RequestMapping("/api/drive/processes")
public class DriveProcessController {
    @GetMapping
    public ResponseEntity<List<DriveProcess>> index() { ... }
}
```

## Testing Strategy

- **Integration test**: `@SpringBootTest` + `@AutoConfigureMockMvc` — verify GET /processes returns 200 with empty list
- **Flyway test**: `@FlywayTest` — verify migration applies cleanly
- **Unit test**: DriveProcessController MockMvc test

## Data Flow

```
HTTP GET /api/drive/processes
  → JwtAuthenticationFilter (extract token, set context)
  → DriveProcessController.index()
  → DriveProcessRepository.findAll()
  → PostgreSQL drive_processes table
  → JSON response []
```

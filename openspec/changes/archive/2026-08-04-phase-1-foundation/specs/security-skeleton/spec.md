# Security Skeleton Specification

## Purpose

Establish Spring Security infrastructure for the DRIVE module. Phase 1 provides a permissive skeleton (permit-all for /api/drive/**) that Phase 8 will tighten with full RBAC.

## Requirements

### Requirement: JWT Authentication Filter

The system MUST extract JWT tokens from the `Authorization: Bearer <token>` header on every request to protected endpoints.

#### Scenario: Valid JWT token

- GIVEN a request with a valid JWT in the Authorization header
- WHEN the JwtAuthenticationFilter processes the request
- THEN the SecurityContext is populated with the authenticated user's claims
- AND the request proceeds to the controller

#### Scenario: Missing Authorization header

- GIVEN a request without an Authorization header
- WHEN the JwtAuthenticationFilter processes the request
- THEN the SecurityContext remains anonymous
- AND the request proceeds (permitted in Phase 1)

#### Scenario: Malformed JWT token

- GIVEN a request with an invalid/malformed JWT in the Authorization header
- WHEN the JwtAuthenticationFilter processes the request
- THEN the SecurityContext remains anonymous
- AND the request proceeds (permitted in Phase 1)

### Requirement: Security Configuration

The system MUST configure Spring Security to permit all requests to `/api/drive/**` without authentication.

#### Scenario: Unauthenticated access to DRIVE endpoints

- GIVEN the application is running with SecurityConfig active
- WHEN an unauthenticated request reaches `/api/drive/processes`
- THEN the request is permitted
- AND a 200 response is returned

#### Scenario: CORS configuration

- GIVEN a cross-origin request from an allowed origin
- WHEN the request reaches any DRIVE endpoint
- THEN CORS headers are included in the response
- AND the browser permits the response

### Requirement: Stateless Session Management

The system MUST use stateless session management (no HTTP session created).

#### Scenario: No session created

- GIVEN an authenticated request to a DRIVE endpoint
- WHEN the request is processed
- THEN no HTTP session is created or maintained
- AND the JWT token is the sole authentication mechanism

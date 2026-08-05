package com.dovindev.driveempresarial.security;

import static org.assertj.core.api.Assertions.assertThat;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import javax.crypto.SecretKey;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Unit tests for JwtAuthenticationFilter. Verifies token extraction, validation, and rejection
 * behavior.
 */
class JwtAuthenticationFilterTest {

  private JwtAuthenticationFilter filter;
  private static final String SECRET =
      "dGhpcyBpcyBhIHNlY3VyZSB0ZXN0IHNlY3JldCBrZXkgZm9yIGp3dCB0b2tlbiBnZW5lcmF0aW9u";
  private SecretKey signingKey;

  @BeforeEach
  void setUp() {
    // Use same key construction as JwtAuthenticationFilter: raw bytes from UTF-8 string
    signingKey = Keys.hmacShaKeyFor(SECRET.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    filter = new JwtAuthenticationFilter(SECRET);
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldNotSetAuthenticationWhenNoHeader() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/drive/processes");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, (req, res) -> {});

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldNotSetAuthenticationWhenAuthorizationHeaderMissing() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/drive/processes");
    request.addHeader("X-Custom-Header", "value");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, (req, res) -> {});

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldNotSetAuthenticationWhenHeaderIsNotBearer() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/drive/processes");
    request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, (req, res) -> {});

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldNotSetAuthenticationWhenTokenIsInvalid() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/drive/processes");
    request.addHeader("Authorization", "Bearer invalid.token.here");
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, (req, res) -> {});

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldNotSetAuthenticationWhenTokenIsExpired() throws Exception {
    String expiredToken =
        Jwts.builder()
            .subject("user@test.com")
            .issuedAt(new java.util.Date(System.currentTimeMillis() - 100000))
            .expiration(new java.util.Date(System.currentTimeMillis() - 50000))
            .signWith(signingKey)
            .compact();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/drive/processes");
    request.addHeader("Authorization", "Bearer " + expiredToken);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, (req, res) -> {});

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNull();
  }

  @Test
  void shouldSetAuthenticationWhenTokenIsValid() throws Exception {
    String validToken =
        Jwts.builder()
            .subject("user@test.com")
            .issuedAt(new java.util.Date())
            .expiration(new java.util.Date(System.currentTimeMillis() + 3600000))
            .signWith(signingKey)
            .compact();

    MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/drive/processes");
    request.addHeader("Authorization", "Bearer " + validToken);
    MockHttpServletResponse response = new MockHttpServletResponse();

    filter.doFilterInternal(request, response, (req, res) -> {});

    assertThat(SecurityContextHolder.getContext().getAuthentication()).isNotNull();
    assertThat(SecurityContextHolder.getContext().getAuthentication().getName())
        .isEqualTo("user@test.com");
  }
}

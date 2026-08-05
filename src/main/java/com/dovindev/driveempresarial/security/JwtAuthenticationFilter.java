package com.dovindev.driveempresarial.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * JWT authentication filter. Extracts and validates Bearer tokens from the Authorization header. On
 * valid token: populates SecurityContext with user claims. On invalid/missing token: logs warning,
 * proceeds without auth (Phase 1 — permit-all). Phase 8 will enforce authentication and add RBAC
 * authorities.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);
  private static final String BEARER_PREFIX = "Bearer ";
  private static final String DEFAULT_SECRET =
      "your-256-bit-secret-key-change-in-production-please";

  private final SecretKey secretKey;

  public JwtAuthenticationFilter(@Value("${app.jwt.secret}") String jwtSecret) {
    if (DEFAULT_SECRET.equals(jwtSecret)) {
      throw new IllegalStateException(
          "JWT secret must be set via JWT_SECRET environment variable. "
              + "The default value is not safe for any environment.");
    }
    this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {

    String header = request.getHeader("Authorization");

    if (header != null && header.startsWith(BEARER_PREFIX)) {
      String token = header.substring(BEARER_PREFIX.length());
      try {
        Claims claims =
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token).getPayload();

        String userId = claims.getSubject();

        UsernamePasswordAuthenticationToken auth =
            new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));

        SecurityContextHolder.getContext().setAuthentication(auth);
        log.debug("Authenticated user: {}", userId);

      } catch (JwtException e) {
        log.warn("Invalid JWT token: {}", e.getMessage());
      }
    }

    filterChain.doFilter(request, response);
  }
}

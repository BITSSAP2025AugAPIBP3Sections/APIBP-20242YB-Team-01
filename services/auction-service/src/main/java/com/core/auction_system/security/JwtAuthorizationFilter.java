package com.core.auction_system.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class JwtAuthorizationFilter extends OncePerRequestFilter {
    private static final Logger log = LoggerFactory.getLogger(JwtAuthorizationFilter.class);

    private final SecretKey secretKey;

    public JwtAuthorizationFilter(String rawSecret) {
        try {
            byte[] keyBytes = rawSecret == null ? new byte[0] : rawSecret.getBytes(StandardCharsets.UTF_8);
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hashed = sha.digest(keyBytes);
            this.secretKey = Keys.hmacShaKeyFor(hashed);
            log.info("JWT Authorization Filter initialized with secure key");
        } catch (Exception ex) {
            log.error("Failed to initialize JWT key", ex);
            throw new IllegalStateException("Failed to initialize JWT key", ex);
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.startsWith("Bearer ")) {
            String token = header.replace("Bearer ", "");
            log.debug("JWT detected on request to {}", request.getRequestURI());
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(token)
                        .getBody();

                String username = claims.getSubject();
                String roleClaim = claims.get("role", String.class);
                Object userIdObj = claims.get("userId");
                Integer userId = null;
                if (userIdObj != null) {
                    if (userIdObj instanceof Number) {
                        userId = ((Number) userIdObj).intValue();
                    } else {
                        try {
                            userId = Integer.parseInt(userIdObj.toString());
                        } catch (Exception ignored) {
                            log.warn("Unable to parse userId from JWT claim: {}", userIdObj);
                        }
                    }
                }

                if (username != null) {
                    String grantedRole = roleClaim == null ? "BUYER" : roleClaim;
                    String authority = grantedRole.startsWith("ROLE_") ? grantedRole : "ROLE_" + grantedRole;

                    log.debug("JWT claims - username: {}, roleClaim: {}, authority: {}, userId: {}",
                            username, roleClaim, authority, userId);

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username, null, List.of(new SimpleGrantedAuthority(authority)));
                    auth.setDetails(userId);
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    log.info("Authenticated user '{}' with role '{}' and userId={} for request: {}",
                            username, authority, userId, request.getRequestURI());
                }
            } catch (Exception e) {
                log.error("JWT validation failed for request {}. Error: {} - {}",
                        request.getRequestURI(), e.getClass().getSimpleName(), e.getMessage());
                log.debug("Full JWT validation error", e);
                SecurityContextHolder.clearContext();
            }
        } else {
            log.debug("No JWT token found for request {}", request.getRequestURI());
        }
        filterChain.doFilter(request, response);
    }
}

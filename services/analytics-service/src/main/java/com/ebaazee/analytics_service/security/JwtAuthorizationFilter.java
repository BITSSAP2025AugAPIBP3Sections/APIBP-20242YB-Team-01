package com.ebaazee.analytics_service.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.crypto.SecretKey;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class JwtAuthorizationFilter extends OncePerRequestFilter {

    // added
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
            log.debug("JWT detected on request {}", request.getRequestURI());
            String token = "<masked>"; // we don’t log sensitive raw tokens
            try {
                Claims claims = Jwts.parserBuilder()
                        .setSigningKey(secretKey)
                        .build()
                        .parseClaimsJws(header.replace("Bearer ", ""))
                        .getBody();

                String username = claims.getSubject();
                String roleClaim = claims.get("role", String.class);
                Object userIdObj = claims.get("userId");
                Integer userId = null;

                if (userIdObj != null) {
                    if (userIdObj instanceof Number) userId = ((Number) userIdObj).intValue();
                    else {
                        try {
                            userId = Integer.parseInt(userIdObj.toString());
                        } catch (Exception ignored) {
                            log.warn("Unable to parse userId from JWT");
                        }
                    }
                }

                if (username != null) {
                    String grantedRole = roleClaim == null ? "BUYER" : roleClaim;
                    String authority = grantedRole.startsWith("ROLE_") ? grantedRole : "ROLE_" + grantedRole;

                    UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                            username, null, List.of(new SimpleGrantedAuthority(authority)));
                    auth.setDetails(userId);
                    SecurityContextHolder.getContext().setAuthentication(auth);

                    log.info("Authenticated user '{}' with role '{}' and userId={}", username, authority, userId);
                }
            } catch (Exception e) {
                log.warn("Invalid JWT detected for request: {}", request.getRequestURI());
                SecurityContextHolder.clearContext();
            }
        } else {
            log.trace("No JWT token provided for request {}", request.getRequestURI());
        }

        filterChain.doFilter(request, response);
    }
}

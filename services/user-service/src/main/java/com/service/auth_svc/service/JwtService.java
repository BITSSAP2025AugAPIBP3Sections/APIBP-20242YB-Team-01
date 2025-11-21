package com.service.auth_svc.service;

import com.service.auth_svc.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

@Service
@Slf4j
public class JwtService {

    private final JwtConfig jwtConfig;
    private SecretKey secretKey;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @PostConstruct
    private void init() {
        try {
            log.info("Initializing JWT secret key...");
            byte[] keyBytes = jwtConfig.getSecret() == null ? new byte[0] : jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
            // Ensure key is 256-bit by hashing the configured secret (safe and deterministic)
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hashed = sha.digest(keyBytes);
            this.secretKey = Keys.hmacShaKeyFor(hashed);
            log.info("JWT secret key initialization complete");
        } catch (Exception ex) {
            log.error("Failed to initialize JWT secret key: {}", ex.getMessage());
            throw new IllegalStateException("Failed to initialize JWT secret key", ex);
        }
    }

    // Generate Access Token (includes role claim)
    public String generateAccessToken(String email, String role) {
        log.debug("Generating access token for email={}, role={}", email, role);
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpirationMs()))
                .signWith(secretKey)
                .compact();
    }

    /**
     * Preferred: include numeric userId in token so other services can read it without remote calls
     */
    public String generateAccessToken(String email, String role, Long userId) {
        log.debug("Generating access token for email={}, role={}, userId={}", email, role, userId);
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getAccessTokenExpirationMs()))
                .signWith(secretKey)
                .compact();
    }

    // Backwards compatible overload
    public String generateAccessToken(String email) {
        log.debug("Generating access token (default BUYER) for email={}", email);
        return generateAccessToken(email, "BUYER");
    }

    // Generate Refresh Token
    public String generateRefreshToken(String email, String role) {
        log.debug("Generating refresh token for email={}, role={}", email, role);
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshTokenExpirationMs()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String email, String role, Long userId) {
        log.debug("Generating refresh token for email={}, role={}, userId={}", email, role, userId);
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .claim("userId", userId)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshTokenExpirationMs()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String email) {
        log.debug("Generating refresh token (default BUYER) for email={}", email);
        return generateRefreshToken(email, "BUYER");
    }

    // Validate token and return claims
    public Claims validateToken(String token) {
        log.trace("Validating JWT token");
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Extract email from token
    public String extractEmail(String token) {
        String email = validateToken(token).getSubject();
        log.debug("Extracted email from token={}", email);
        return email;
    }

    // Extract role from token
    public String extractRole(String token) {
        String role = validateToken(token).get("role", String.class);
        log.debug("Extracted role from token={}", role);
        return role;
    }

    // Extract user id from token (may be null)
    public Long extractUserId(String token) {
        Object v = validateToken(token).get("userId");
        if (v == null) {
            log.debug("No userId claim found in token");
            return null;
        }
        if (v instanceof Number) {
            Long uid = ((Number) v).longValue();
            log.debug("Extracted userId={} from token", uid);
            return uid;
        }
        try {
            Long uid = Long.parseLong(v.toString());
            log.debug("Parsed userId={} from token", uid);
            return uid;
        } catch (Exception ex) {
            log.warn("Failed to parse userId claim from token");
            return null;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        boolean valid = (email != null && email.equals(userDetails.getUsername()));
        log.debug("Token validity for user {} -> {}", userDetails.getUsername(), valid);
        return valid;
    }
}

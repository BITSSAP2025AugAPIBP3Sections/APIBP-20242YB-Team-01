package com.service.auth_svc.service;

import com.service.auth_svc.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;

@Service
public class JwtService {

    private final JwtConfig jwtConfig;
    private SecretKey secretKey;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    @PostConstruct
    private void init() {
        try {
            byte[] keyBytes = jwtConfig.getSecret() == null ? new byte[0] : jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
            // Ensure key is 256-bit by hashing the configured secret (safe and deterministic)
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            byte[] hashed = sha.digest(keyBytes);
            this.secretKey = Keys.hmacShaKeyFor(hashed);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to initialize JWT secret key", ex);
        }
    }

    // Generate Access Token
    // Generate Access Token (includes role claim)
    public String generateAccessToken(String email, String role) {
        // Backwards compatible: no userId provided
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
        Jwts.builder();
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
        return generateAccessToken(email, "BUYER");
    }

    // Generate Refresh Token
    public String generateRefreshToken(String email, String role) {
        return Jwts.builder()
                .setSubject(email)
                .claim("role", role)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + jwtConfig.getRefreshTokenExpirationMs()))
                .signWith(secretKey)
                .compact();
    }

    public String generateRefreshToken(String email, String role, Long userId) {
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
        return generateRefreshToken(email, "BUYER");
    }

    // Validate token and return claims
    public Claims validateToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Extract email from token
    public String extractEmail(String token) {
        return validateToken(token).getSubject();
    }

    // Extract role from token
    public String extractRole(String token) {
        return validateToken(token).get("role", String.class);
    }

    // Extract user id from token (may be null)
    public Long extractUserId(String token) {
        Object v = validateToken(token).get("userId");
        if (v == null) return null;
        if (v instanceof Number) return ((Number) v).longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (Exception ex) {
            return null;
        }
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        return (email != null && email.equals(userDetails.getUsername()));
    }
}

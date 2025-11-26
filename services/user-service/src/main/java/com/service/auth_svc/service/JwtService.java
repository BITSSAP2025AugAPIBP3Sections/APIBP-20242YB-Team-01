package com.service.auth_svc.service;

import com.service.auth_svc.config.JwtConfig;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Date;
import javax.crypto.SecretKey;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class JwtService {

    private final JwtConfig jwtConfig;
    private SecretKey secretKey;

    public JwtService(JwtConfig jwtConfig) {
        this.jwtConfig = jwtConfig;
    }

    /**
     * Initializes the JWT signing key from configuration.
     * 
     * <p>The configured secret is hashed using SHA-256 to ensure it meets the
     * 256-bit minimum requirement for HS256 algorithm. This approach is both
     * deterministic (same secret always produces same key) and secure.</p>
     * 
     * @throws IllegalStateException if key initialization fails
     */
    @PostConstruct
    private void init() {
        try {
            log.info("Initializing JWT secret key...");
            byte[] keyBytes = jwtConfig.getSecret() == null ? new byte[0] :
                    jwtConfig.getSecret().getBytes(StandardCharsets.UTF_8);
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

    /**
     * Generates a JWT access token with email and role claims.
     * 
     * <p><b>Note:</b> Prefer using the overload that includes userId for better
     * inter-service communication.</p>
     * 
     * @param email user's email address (token subject)
     * @param role user's role (e.g., "BUYER", "SELLER", "ADMIN")
     * @return signed JWT access token string
     */
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
     * Generates a JWT access token with email, role, and userId claims.
     * 
     * <p><b>Recommended:</b> Including userId in the token allows other microservices
     * to identify the user without making additional API calls to the user service.</p>
     * 
     * @param email user's email address (token subject)
     * @param role user's role (e.g., "BUYER", "SELLER", "ADMIN")
     * @param userId numeric user identifier
     * @return signed JWT access token string
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

    /**
     * Generates a JWT access token with email only, defaulting role to BUYER.
     * 
     * <p><b>Legacy Method:</b> Provided for backward compatibility. New code should
     * use overloads that explicitly specify role and userId.</p>
     * 
     * @param email user's email address (token subject)
     * @return signed JWT access token string with default BUYER role
     */
    public String generateAccessToken(String email) {
        log.debug("Generating access token (default BUYER) for email={}", email);
        return generateAccessToken(email, "BUYER");
    }

    /**
     * Generates a JWT refresh token with email and role claims.
     * 
     * <p><b>Note:</b> In production, opaque refresh tokens stored in database are preferred
     * over JWT refresh tokens for better security and revocation capability.</p>
     * 
     * @param email user's email address (token subject)
     * @param role user's role
     * @return signed JWT refresh token string
     */
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

    /**
     * Generates a JWT refresh token with email, role, and userId claims.
     * 
     * @param email user's email address (token subject)
     * @param role user's role
     * @param userId numeric user identifier
     * @return signed JWT refresh token string
     */
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

    /**
     * Generates a JWT refresh token with email only, defaulting role to BUYER.
     * 
     * @param email user's email address (token subject)
     * @return signed JWT refresh token string with default BUYER role
     */
    public String generateRefreshToken(String email) {
        log.debug("Generating refresh token (default BUYER) for email={}", email);
        return generateRefreshToken(email, "BUYER");
    }

    /**
     * Validates a JWT token and extracts its claims.
     * 
     * <p>Verifies the token signature and expiration. Throws exceptions if:</p>
     * <ul>
     *   <li>Signature is invalid (SignatureException)</li>
     *   <li>Token is expired (ExpiredJwtException)</li>
     *   <li>Token is malformed (MalformedJwtException)</li>
     * </ul>
     * 
     * @param token the JWT token string to validate
     * @return Claims object containing all token claims
     * @throws io.jsonwebtoken.JwtException if validation fails
     */
    public Claims validateToken(String token) {
        log.trace("Validating JWT token");
        return Jwts.parserBuilder()
                .setSigningKey(secretKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extracts the email address (subject) from a JWT token.
     * 
     * @param token the JWT token string
     * @return the email address from the token's subject claim
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
    public String extractEmail(String token) {
        String email = validateToken(token).getSubject();
        log.debug("Extracted email from token={}", email);
        return email;
    }

    /**
     * Extracts the user role from a JWT token.
     * 
     * @param token the JWT token string
     * @return the role string from the token's role claim
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
    public String extractRole(String token) {
        String role = validateToken(token).get("role", String.class);
        log.debug("Extracted role from token={}", role);
        return role;
    }

    /**
     * Extracts the user ID from a JWT token.
     * 
     * <p>Returns null if the userId claim is not present (older tokens may not include it).
     * Handles both numeric and string representations of the user ID.</p>
     * 
     * @param token the JWT token string
     * @return the user ID from the token's userId claim, or null if not present
     * @throws io.jsonwebtoken.JwtException if token is invalid
     */
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

    /**
     * Validates that a JWT token belongs to the specified user.
     * 
     * <p>Checks that the token's subject (email) matches the username in UserDetails.
     * Token signature and expiration are validated as part of email extraction.</p>
     * 
     * @param token the JWT token string to validate
     * @param userDetails Spring Security user details to match against
     * @return true if token is valid and belongs to the user, false otherwise
     */
    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String email = extractEmail(token);
        boolean valid = (email != null && email.equals(userDetails.getUsername()));
        log.debug("Token validity for user {} -> {}", userDetails.getUsername(), valid);
        return valid;
    }
}

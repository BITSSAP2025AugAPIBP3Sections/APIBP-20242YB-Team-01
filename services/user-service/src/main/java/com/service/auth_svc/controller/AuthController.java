package com.service.auth_svc.controller;

import com.service.auth_svc.dto.*;
import com.service.auth_svc.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthController {

    // added
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * POST /api/auth/v1/register
     * User Registration
     */
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Attempting registration for email={}", request.getEmail());
        authService.register(request);
        log.info("User registered successfully for email={}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
        .body(Map.of("message", "User registered successfully"));

    }

    /**
     * POST /api/auth/v1/login
     * User Login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email={}", request.getEmail());
        LoginResponse response = authService.login(request);
        log.info("Login successful for email={}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/v1/refresh-token
     * Refresh JWT Token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        log.debug("Refreshing access token using refresh token");
        LoginResponse response = authService.refreshToken(refreshToken);
        log.info("Refresh token accepted and new access token issued");
        return ResponseEntity.ok(response);
    }

    /**
     * POST /api/auth/v1/revoke
     * Revoke refresh token (logout)
     */
    @PostMapping("/revoke")
    public ResponseEntity<String> revokeToken(@RequestBody RevokeRequest request) {
        log.info("Revoking refresh token");
        authService.revokeRefreshToken(request.getRefreshToken());
        log.debug("Refresh token revoked");
        return ResponseEntity.ok("Refresh token revoked");
    }

    /**
     * POST /api/auth/v1/revoke-all
     * Revoke all refresh tokens for the authenticated user (logout everywhere)
     */
    @PostMapping("/revoke-all")
    public ResponseEntity<String> revokeAllForCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        log.warn("Revoking ALL refresh tokens for email={}", email);
        authService.revokeAllTokensForUser(email);
        return ResponseEntity.ok("All refresh tokens revoked for user: " + email);
    }

    /**
     * GET /api/auth/v1/me
     * Get current authenticated user's profile
     */
    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        log.debug("Fetching profile for current user email={}", email);
        UserProfileDTO profile = authService.getUserProfile(email);
        log.info("Fetched profile for current user email={}", email);
        return ResponseEntity.ok(profile);
    }

    /**
     * GET /api/auth/v1/users/{id}
     * Get user profile by user ID
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        log.debug("Fetching user profile by id={}", id);
        UserProfileDTO profile = authService.getUserProfileById(id);
        log.info("Fetched user profile for id={}", id);
        return ResponseEntity.ok(profile);
    }
}

package com.service.auth_svc.controller;

import com.service.auth_svc.dto.*;
import com.service.auth_svc.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    // added
    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * User Registration
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Attempting registration for email={}", request.getEmail());
        authService.register(request);
        log.info("User registered successfully for email={}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    /**
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
     * Revoke all refresh tokens for the authenticated user (logout everywhere)
     */
    @PostMapping("/revoke-all")
    public ResponseEntity<String> revokeAllForCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        log.warn("Revoking ALL refresh tokens for email={}", email);
        authService.revokeAllTokensForUser(email);
        return ResponseEntity.ok("All refresh tokens revoked for user: " + email);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        log.debug("Fetching profile for current user email={}", email);
        UserProfileDTO profile = authService.getUserProfile(email);
        log.info("Fetched profile for current user email={}", email);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        log.debug("Fetching user profile by id={}", id);
        UserProfileDTO profile = authService.getUserProfileById(id);
        log.info("Fetched user profile for id={}", id);
        return ResponseEntity.ok(profile);
    }
}

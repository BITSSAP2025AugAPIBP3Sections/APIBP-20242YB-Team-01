package com.service.auth_svc.controller;

import com.service.auth_svc.dto.*;
import com.service.auth_svc.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    /**
     * User Registration
     */
    @PostMapping("/register")
    public ResponseEntity<String> register(@Valid @RequestBody RegisterRequest request) {
        authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body("User registered successfully");
    }

    /**
     * User Login
     */
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    /**
     * Refresh JWT Token
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        LoginResponse response = authService.refreshToken(refreshToken);
        return ResponseEntity.ok(response);
    }

    /**
     * Revoke refresh token (logout)
     */
    @PostMapping("/revoke")
    public ResponseEntity<String> revokeToken(@RequestBody RevokeRequest request) {
        authService.revokeRefreshToken(request.getRefreshToken());
        return ResponseEntity.ok("Refresh token revoked");
    }

    /**
     * Revoke all refresh tokens for the authenticated user (logout everywhere)
     */
    @PostMapping("/revoke-all")
    public ResponseEntity<String> revokeAllForCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        authService.revokeAllTokensForUser(email);
        return ResponseEntity.ok("All refresh tokens revoked for user: " + email);
    }

    @GetMapping("/me")
    public ResponseEntity<UserProfileDTO> getCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        UserProfileDTO profile = authService.getUserProfile(email);
        return ResponseEntity.ok(profile);
    }

    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        UserProfileDTO profile = authService.getUserProfileById(id);
        return ResponseEntity.ok(profile);
    }
}

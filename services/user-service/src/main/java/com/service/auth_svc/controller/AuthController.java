package com.service.auth_svc.controller;

import com.service.auth_svc.dto.LoginRequest;
import com.service.auth_svc.dto.LoginResponse;
import com.service.auth_svc.dto.RegisterRequest;
import com.service.auth_svc.dto.RevokeRequest;
import com.service.auth_svc.dto.UserProfileDTO;
import com.service.auth_svc.service.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for authentication and user management operations.
 * 
 * <p>Provides endpoints for user registration, login, token management,
 * and profile retrieval. All endpoints follow RESTful conventions and
 * return standardized JSON responses.</p>
 * 
 * <p><b>Base Path:</b> {@code /api/auth/v1}</p>
 * 
 * <p><b>Security:</b> Most endpoints require JWT authentication via
 * the Authorization header (Bearer token), except for registration,
 * login, and token refresh which are publicly accessible.</p>
 * 
 * @author APIBP Team
 * @version 1.0
 * @since 2025-11-26
 */
@RestController
@RequestMapping("/api/auth/v1")
@RequiredArgsConstructor
public class AuthController {

    private static final Logger log = LoggerFactory.getLogger(AuthController.class);

    private final AuthService authService;

    /**
     * Registers a new user account.
     * 
     * <p><b>Endpoint:</b> {@code POST /api/auth/v1/register}</p>
     * 
     * <p><b>Request Body:</b> RegisterRequest JSON with email, password, fullName, and optional role</p>
     * 
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li><b>201 Created:</b> User successfully registered</li>
     *   <li><b>400 Bad Request:</b> Validation errors (invalid email format, weak password, etc.)</li>
     *   <li><b>409 Conflict:</b> Email already exists</li>
     * </ul>
     * 
     * <p><b>Security:</b> Public endpoint, no authentication required</p>
     * 
     * @param request validated registration request containing user details
     * @return ResponseEntity with success message and HTTP 201 status
     */
    @CrossOrigin(
    origins = "http://localhost:5173",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Attempting registration for email={}", request.getEmail());
        authService.register(request);
        log.info("User registered successfully for email={}", request.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "User registered successfully"));
    }

    /**
     * Authenticates a user and issues JWT tokens.
     * 
     * <p><b>Endpoint:</b> {@code POST /api/auth/v1/login}</p>
     * 
     * <p><b>Request Body:</b> LoginRequest JSON with email and password</p>
     * 
     * <p><b>Response Body:</b> LoginResponse containing:</p>
     * <ul>
     *   <li><b>accessToken:</b> JWT token for API authentication (short-lived)</li>
     *   <li><b>refreshToken:</b> Opaque token for obtaining new access tokens (long-lived)</li>
     * </ul>
     * 
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li><b>200 OK:</b> Authentication successful, tokens returned</li>
     *   <li><b>401 Unauthorized:</b> Invalid credentials</li>
     *   <li><b>400 Bad Request:</b> Validation errors</li>
     * </ul>
     * 
     * <p><b>Security:</b> Public endpoint, no authentication required</p>
     * 
     * @param request validated login request containing email and password
     * @return ResponseEntity with LoginResponse containing access and refresh tokens
     */
    @CrossOrigin(
    origins = "http://localhost:5173",
    methods = {RequestMethod.GET, RequestMethod.POST, RequestMethod.OPTIONS}
)
    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Login request for email={}", request.getEmail());
        LoginResponse response = authService.login(request);
        log.info("Login successful for email={}", request.getEmail());
        return ResponseEntity.ok(response);
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * 
     * <p><b>Endpoint:</b> {@code POST /api/auth/v1/refresh-token}</p>
     * 
     * <p><b>Request Parameters:</b> refreshToken (query parameter)</p>
     * 
     * <p><b>Response Body:</b> LoginResponse with new access and refresh tokens</p>
     * 
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li><b>200 OK:</b> New tokens issued successfully</li>
     *   <li><b>401 Unauthorized:</b> Invalid or expired refresh token</li>
     * </ul>
     * 
     * <p><b>Security:</b> Public endpoint. Implements token rotation - the old refresh
     * token is invalidated and a new one is issued for enhanced security.</p>
     * 
     * @param refreshToken the opaque refresh token issued during login
     * @return ResponseEntity with LoginResponse containing new tokens
     */
    @PostMapping("/refresh-token")
    public ResponseEntity<LoginResponse> refreshToken(@RequestParam("refreshToken") String refreshToken) {
        log.debug("Refreshing access token using refresh token");
        LoginResponse response = authService.refreshToken(refreshToken);
        log.info("Refresh token accepted and new access token issued");
        return ResponseEntity.ok(response);
    }

    /**
     * Revokes a specific refresh token (logout from current device/session).
     * 
     * <p><b>Endpoint:</b> {@code POST /api/auth/v1/revoke}</p>
     * 
     * <p><b>Request Body:</b> RevokeRequest JSON with refreshToken</p>
     * 
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li><b>200 OK:</b> Token revoked successfully</li>
     * </ul>
     * 
     * <p><b>Security:</b> Public endpoint. The refresh token to revoke is provided
     * in the request body.</p>
     * 
     * @param request revoke request containing the refresh token to invalidate
     * @return ResponseEntity with success message
     */
    @PostMapping("/revoke")
    public ResponseEntity<String> revokeToken(@RequestBody RevokeRequest request) {
        log.info("Revoking refresh token");
        authService.revokeRefreshToken(request.getRefreshToken());
        log.debug("Refresh token revoked");
        return ResponseEntity.ok("Refresh token revoked");
    }

    /**
     * Revokes all refresh tokens for the authenticated user (logout from all devices).
     * 
     * <p><b>Endpoint:</b> {@code POST /api/auth/v1/revoke-all}</p>
     * 
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li><b>200 OK:</b> All tokens revoked successfully</li>
     *   <li><b>401 Unauthorized:</b> User not authenticated</li>
     * </ul>
     * 
     * <p><b>Security:</b> Requires JWT authentication. This is a critical security
     * operation useful when a user suspects their account has been compromised or
     * when changing passwords.</p>
     * 
     * @param authentication Spring Security authentication object (auto-injected)
     * @return ResponseEntity with success message
     */
    @PostMapping("/revoke-all")
    public ResponseEntity<String> revokeAllForCurrentUser(Authentication authentication) {
        String email = authentication.getName();
        log.warn("Revoking ALL refresh tokens for email={}", email);
        authService.revokeAllTokensForUser(email);
        return ResponseEntity.ok("All refresh tokens revoked for user: " + email);
    }

    /**
     * Retrieves the profile of the currently authenticated user.
     * 
     * <p><b>Endpoint:</b> {@code GET /api/auth/v1/me}</p>
     * 
     * <p><b>Response Body:</b> UserProfileDTO containing id, fullName, email, role,
     * enabled status, and createdAt timestamp</p>
     * 
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li><b>200 OK:</b> Profile retrieved successfully</li>
     *   <li><b>401 Unauthorized:</b> User not authenticated or token invalid</li>
     * </ul>
     * 
     * <p><b>Security:</b> Requires JWT authentication. The user is identified from
     * the JWT token in the Authorization header.</p>
     * 
     * @param authentication Spring Security authentication object (auto-injected)
     * @return ResponseEntity with UserProfileDTO
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
     * Retrieves a user profile by their unique user ID.
     * 
     * <p><b>Endpoint:</b> {@code GET /api/auth/v1/users/{id}}</p>
     * 
     * <p><b>Path Variables:</b> id - the unique user identifier</p>
     * 
     * <p><b>Response Body:</b> UserProfileDTO containing user information</p>
     * 
     * <p><b>Responses:</b></p>
     * <ul>
     *   <li><b>200 OK:</b> Profile retrieved successfully</li>
     *   <li><b>404 Not Found:</b> User with specified ID does not exist</li>
     *   <li><b>401 Unauthorized:</b> User not authenticated</li>
     * </ul>
     * 
     * <p><b>Security:</b> Requires JWT authentication. This endpoint is designed
     * for inter-service communication and admin operations.</p>
     * 
     * @param id the unique identifier of the user to retrieve
     * @return ResponseEntity with UserProfileDTO
     */
    @GetMapping("/users/{id}")
    public ResponseEntity<UserProfileDTO> getUserById(@PathVariable Long id) {
        log.debug("Fetching user profile by id={}", id);
        UserProfileDTO profile = authService.getUserProfileById(id);
        log.info("Fetched user profile for id={}", id);
        return ResponseEntity.ok(profile);
    }
}

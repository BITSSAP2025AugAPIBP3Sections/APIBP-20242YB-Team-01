package com.service.auth_svc.service;

import com.service.auth_svc.config.JwtConfig;
import com.service.auth_svc.dto.LoginRequest;
import com.service.auth_svc.dto.LoginResponse;
import com.service.auth_svc.dto.RegisterRequest;
import com.service.auth_svc.dto.UserProfileDTO;
import com.service.auth_svc.entity.User;
import com.service.auth_svc.entity.UserRole;
import com.service.auth_svc.exception.CustomException;
import com.service.auth_svc.repository.UserRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Core authentication and user management service.
 * 
 * <p>This service handles all authentication-related operations including user registration,
 * login, token management, and user profile retrieval. It implements secure authentication
 * flows with JWT-based access tokens and opaque refresh tokens for enhanced security.</p>
 * 
 * <p><b>Security Features:</b></p>
 * <ul>
 *   <li>Password encryption using BCrypt</li>
 *   <li>JWT access tokens with user claims (email, role, userId)</li>
 *   <li>Opaque refresh tokens with database persistence</li>
 *   <li>Token rotation on refresh for enhanced security</li>
 *   <li>Role-based access control (RBAC)</li>
 * </ul>
 * 
 * <p><b>Supported User Roles:</b> BUYER, SELLER, ADMIN</p>
 * 
 * @author APIBP Team
 * @version 1.0
 * @since 2025-11-26
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtConfig jwtConfig;
    private final RefreshTokenService refreshTokenService;

    /**
     * Registers a new user in the system.
     * 
     * <p>Validates that the email is unique, encodes the password using BCrypt,
     * and assigns the specified role (defaults to BUYER if not provided or invalid).</p>
     * 
     * @param request the registration request containing email, password, full name, and optional role
     * @throws com.service.auth_svc.exception.UserAlreadyExistsException if email is already registered
     * @throws com.service.auth_svc.exception.InvalidRoleException if the provided role is invalid
     */
    public void register(RegisterRequest request) {
        log.debug("Registration attempt for email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: user already exists with email={}", request.getEmail());
            throw new com.service.auth_svc.exception.UserAlreadyExistsException(request.getEmail());
        }

        // Determine role, default to BUYER
        UserRole role = parseRoleOrDefault(request.getRole());

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .enabled(true)
                .role(role)
                .build();

        userRepository.save(user);
        log.info("Registered new user: {} with role {}", user.getEmail(), user.getRole());
    }

    /**
     * Parses the role string and returns the corresponding UserRole enum.
     * Defaults to BUYER if role is null, blank, or invalid.
     * 
     * @param roleStr the role string to parse
     * @return the corresponding UserRole enum value
     * @throws com.service.auth_svc.exception.InvalidRoleException if roleStr is invalid
     */
    private UserRole parseRoleOrDefault(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) {
            return UserRole.BUYER;
        }
        try {
            return UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid role signup request: {}", roleStr);
            throw new com.service.auth_svc.exception.InvalidRoleException(roleStr);
        }
    }

    /**
     * Authenticates a user and issues JWT tokens.
     * 
     * <p>Validates credentials, generates a JWT access token containing user claims
     * (email, role, userId), and creates an opaque refresh token persisted in the database.</p>
     * 
     * <p><b>Token Details:</b></p>
     * <ul>
     *   <li><b>Access Token:</b> JWT with email, role, and userId claims. Used for API authentication.</li>
     *   <li><b>Refresh Token:</b> Opaque random token stored in database. Used to obtain new access tokens.</li>
     * </ul>
     * 
     * @param request the login request containing email and password
     * @return LoginResponse containing both access and refresh tokens
     * @throws com.service.auth_svc.exception.InvalidCredentialsException if credentials are invalid
     */
    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed for email={} (no such user)", request.getEmail());
                    return new com.service.auth_svc.exception.InvalidCredentialsException();
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed for email={} (incorrect password)", request.getEmail());
            throw new com.service.auth_svc.exception.InvalidCredentialsException();
        }

        // Include numeric user id in token so other services can read it without extra calls
        Long uid = user.getId();
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name(), uid);

        // Create opaque refresh token and persist
        String opaqueRefresh = null;
        try {
            long refreshMs = jwtConfig.getRefreshTokenExpirationMs();
            opaqueRefresh = refreshTokenService.createAndPersistRandomToken(user,
                    java.time.Instant.now().plusMillis(refreshMs));
            log.debug("Refresh token created for user={} (length={})", user.getEmail(),
                    opaqueRefresh != null ? opaqueRefresh.length() : 0);
        } catch (Exception ex) {
            log.warn("Failed to create persistent refresh token for user {}: {}", user.getEmail(), ex.getMessage());
        }

        log.info("Login successful for email={}", user.getEmail());
        LoginResponse response = new LoginResponse(accessToken, opaqueRefresh);
        response.setRole(user.getRole().name());
        response.setId(user.getId().intValue());
        return response;
    }

    /**
     * Retrieves the user profile for the given email address.
     * 
     * @param email the email address of the user
     * @return UserProfileDTO containing user information (excluding sensitive data like password)
     * @throws com.service.auth_svc.exception.UserNotFoundException if user does not exist
     */
    public UserProfileDTO getUserProfile(String email) {
        log.debug("Fetching profile for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Profile requested for non-existing email={}", email);
                    return new com.service.auth_svc.exception.UserNotFoundException(email);
                });

        return mapToProfileDTO(user);
    }

    /**
     * Retrieves the user profile by user ID.
     * 
     * <p>This method is used by other microservices to fetch user details
     * without requiring email-based lookups.</p>
     * 
     * @param userId the unique identifier of the user
     * @return UserProfileDTO containing user information
     * @throws com.service.auth_svc.exception.UserNotFoundException if user does not exist
     */
    public UserProfileDTO getUserProfileById(Long userId) {
        log.debug("Fetching profile for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Profile requested for non-existing userId={}", userId);
                    return new com.service.auth_svc.exception.UserNotFoundException("User ID: " + userId);
                });

        return mapToProfileDTO(user);
    }

    /**
     * Maps User entity to UserProfileDTO, excluding sensitive information.
     * 
     * @param user the user entity to map
     * @return UserProfileDTO with safe user information
     */
    private UserProfileDTO mapToProfileDTO(User user) {
        return UserProfileDTO.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole())
                .enabled(user.isEnabled())
                .createdAt(user.getCreatedAt())
                .build();
    }

    /**
     * Refreshes an expired access token using a valid refresh token.
     * 
     * <p>Implements token rotation for enhanced security: the old refresh token
     * is revoked and a new one is issued along with a new access token. This prevents
     * refresh token reuse and limits the impact of token theft.</p>
     * 
     * @param refreshToken the opaque refresh token issued during login
     * @return LoginResponse containing new access and refresh tokens
     * @throws CustomException if the refresh token is invalid or expired (HTTP 401)
     */
    public LoginResponse refreshToken(String refreshToken) {
        log.debug("Refresh token attempt: {}", refreshToken != null ? "[provided]" : "[missing]");

        try {
            com.service.auth_svc.entity.RefreshToken stored = refreshTokenService.verifyRefreshToken(refreshToken);
            User user = stored.getUser();

            String newAccessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name());

            // Revoke old refresh token and issue a new opaque token (rotation)
            refreshTokenService.revokeByToken(refreshToken);
            long refreshMs = jwtConfig.getRefreshTokenExpirationMs();
            String newOpaque = refreshTokenService.createAndPersistRandomToken(user,
                    java.time.Instant.now().plusMillis(refreshMs));

            log.info("Refresh token rotated for user={}", user.getEmail());
            LoginResponse response = new LoginResponse(newAccessToken, newOpaque);
            response.setRole(user.getRole().name());
            response.setId(user.getId().intValue());
            return response;
        } catch (Exception ex) {
            log.warn("Invalid refresh token presented: {}", ex.getMessage());
            throw new CustomException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Revokes all refresh tokens for a user, effectively logging them out from all devices.
     * 
     * <p>This is a security feature that allows users to invalidate all active sessions
     * across devices. Useful in case of suspected account compromise or when changing passwords.</p>
     * 
     * @param email the email address of the user whose tokens should be revoked
     * @throws com.service.auth_svc.exception.UserNotFoundException if user does not exist
     */
    @Transactional
    public void revokeAllTokensForUser(String email) {
        log.warn("Revoking ALL refresh tokens for user={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.service.auth_svc.exception.UserNotFoundException(email));

        refreshTokenService.revokeAllForUser(user);
    }

    /**
     * Revokes a specific refresh token, invalidating it for future use.
     * 
     * <p>This is used during logout to invalidate the current session token.</p>
     * 
     * @param refreshToken the opaque refresh token to revoke
     */
    public void revokeRefreshToken(String refreshToken) {
        log.debug("Revoking single refresh token");
        refreshTokenService.revokeByToken(refreshToken);
    }

    /**
     * Creates an opaque refresh token for a user identified by email.
     * 
     * <p>Used primarily by OAuth2 authentication flow to issue refresh tokens
     * for users authenticated via external providers (Google, GitHub, etc.).</p>
     * 
     * @param email the email address of the user
     * @return the generated opaque refresh token string
     * @throws com.service.auth_svc.exception.UserNotFoundException if user does not exist
     */
    public String createOpaqueRefreshTokenForEmail(String email) {
        log.debug("Creating opaque refresh token for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.service.auth_svc.exception.UserNotFoundException(email));

        long refreshMs = jwtConfig.getRefreshTokenExpirationMs();
        return refreshTokenService.createAndPersistRandomToken(user, Instant.now().plusMillis(refreshMs));
    }

    /**
     * Retrieves the role of a user by their email address.
     * 
     * <p>This is a helper method used by OAuth2 authentication flow and other
     * components that need to determine user permissions.</p>
     * 
     * @param email the email address of the user
     * @return the role name as a string (e.g., "BUYER", "SELLER", "ADMIN")
     * @throws com.service.auth_svc.exception.UserNotFoundException if user does not exist
     */
    public String getRoleForEmail(String email) {
        log.debug("Fetching role for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new com.service.auth_svc.exception.UserNotFoundException(email));

        return user.getRole().name();
    }
}

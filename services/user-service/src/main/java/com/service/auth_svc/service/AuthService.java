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
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

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
     * User registration
     */
    public void register(RegisterRequest request) {
        log.debug("Registration attempt for email={}", request.getEmail());

        if (userRepository.existsByEmail(request.getEmail())) {
            log.warn("Registration failed: user already exists with email={}", request.getEmail());
            throw new CustomException("User already exists with email: " + request.getEmail(), HttpStatus.CONFLICT);
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

    private UserRole parseRoleOrDefault(String roleStr) {
        if (roleStr == null || roleStr.isBlank()) return UserRole.BUYER;
        try {
            return UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            log.warn("Invalid role signup request: {}", roleStr);
            throw new CustomException("Invalid role: " + roleStr + ". Allowed: ADMIN, BUYER, SELLER", HttpStatus.BAD_REQUEST);
        }
    }

    /**
     * User login
     */
    public LoginResponse login(LoginRequest request) {
        log.debug("Login attempt for email={}", request.getEmail());

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    log.warn("Login failed for email={} (no such user)", request.getEmail());
                    return new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED);
                });

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            log.warn("Login failed for email={} (incorrect password)", request.getEmail());
            throw new CustomException("Invalid credentials", HttpStatus.UNAUTHORIZED);
        }

        // Include numeric user id in token so other services can read it without extra calls
        Long uid = user.getId();
        String accessToken = jwtService.generateAccessToken(user.getEmail(), user.getRole().name(), uid);

        // Create opaque refresh token and persist
        String opaqueRefresh = null;
        try {
            long refreshMs = jwtConfig.getRefreshTokenExpirationMs();
            opaqueRefresh = refreshTokenService.createAndPersistRandomToken(user, java.time.Instant.now().plusMillis(refreshMs));
            log.debug("Refresh token created for user={} (length={})", user.getEmail(), opaqueRefresh != null ? opaqueRefresh.length() : 0);
        } catch (Exception ex) {
            log.warn("Failed to create persistent refresh token for user {}: {}", user.getEmail(), ex.getMessage());
        }

        log.info("Login successful for email={}", user.getEmail());
        return new LoginResponse(accessToken, opaqueRefresh);
    }

    public UserProfileDTO getUserProfile(String email) {
        log.debug("Fetching profile for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Profile requested for non-existing email={}", email);
                    return new RuntimeException("User not found");
                });

        return mapToProfileDTO(user);
    }

    /**
     * Get user profile by ID
     */
    public UserProfileDTO getUserProfileById(Long userId) {
        log.debug("Fetching profile for userId={}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> {
                    log.warn("Profile requested for non-existing userId={}", userId);
                    return new RuntimeException("User not found");
                });

        return mapToProfileDTO(user);
    }

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
     * Refresh JWT token
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
            String newOpaque = refreshTokenService.createAndPersistRandomToken(user, java.time.Instant.now().plusMillis(refreshMs));

            log.info("Refresh token rotated for user={}", user.getEmail());
            return new LoginResponse(newAccessToken, newOpaque);
        } catch (Exception ex) {
            log.warn("Invalid refresh token presented: {}", ex.getMessage());
            throw new CustomException("Invalid refresh token", HttpStatus.UNAUTHORIZED);
        }
    }

    /**
     * Revoke all refresh tokens for a given user (logout everywhere)
     */
    @Transactional
    public void revokeAllTokensForUser(String email) {
        log.warn("Revoking ALL refresh tokens for user={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        refreshTokenService.revokeAllForUser(user);
    }

    public void revokeRefreshToken(String refreshToken) {
        log.debug("Revoking single refresh token");
        refreshTokenService.revokeByToken(refreshToken);
    }

    public String createOpaqueRefreshTokenForEmail(String email) {
        log.debug("Creating opaque refresh token for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        long refreshMs = jwtConfig.getRefreshTokenExpirationMs();
        return refreshTokenService.createAndPersistRandomToken(user, Instant.now().plusMillis(refreshMs));
    }

    // Helper for other components
    public String getRoleForEmail(String email) {
        log.debug("Fetching role for email={}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new CustomException("User not found", HttpStatus.NOT_FOUND));

        return user.getRole().name();
    }
}

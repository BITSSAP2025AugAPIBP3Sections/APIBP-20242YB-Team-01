package com.service.auth_svc.service;

import com.service.auth_svc.entity.RefreshToken;
import com.service.auth_svc.entity.User;
import com.service.auth_svc.exception.CustomException;
import com.service.auth_svc.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(User user, String token, Instant expiryDate) {
        log.debug("Creating refresh token for user={}", user.getEmail());

        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();

        RefreshToken saved = refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created for user={} (expires at {})", user.getEmail(), expiryDate);
        return saved;
    }

    public RefreshToken verifyRefreshToken(String token) {
        log.debug("Verifying refresh token");

        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found");
                    return new CustomException("Refresh token not found", HttpStatus.UNAUTHORIZED);
                });

        if (rt.isRevoked()) {
            log.warn("Refresh token is revoked for user={}", rt.getUser().getEmail());
            throw new CustomException("Refresh token revoked", HttpStatus.UNAUTHORIZED);
        }

        if (rt.getExpiryDate().isBefore(Instant.now())) {
            log.warn("Refresh token expired for user={}", rt.getUser().getEmail());
            throw new CustomException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        log.debug("Refresh token accepted for user={}", rt.getUser().getEmail());
        return rt;
    }

    public void revokeByToken(String token) {
        log.debug("Revoking refresh token");
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
            log.info("Refresh token revoked for user={}", rt.getUser().getEmail());
        });
    }

    public void revokeAllForUser(User user) {
        log.warn("Revoking ALL refresh tokens for user={}", user.getEmail());
        refreshTokenRepository.deleteByUser(user);
    }

    public String createAndPersistRandomToken(User user, Instant expiryDate) {
        String token = UUID.randomUUID().toString();
        log.debug("Generating random refresh token for user={}", user.getEmail());
        createRefreshToken(user, token, expiryDate);
        return token;
    }
}

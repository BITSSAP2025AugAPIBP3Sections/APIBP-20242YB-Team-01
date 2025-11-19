package com.service.auth_svc.service;

import com.service.auth_svc.entity.RefreshToken;
import com.service.auth_svc.entity.User;
import com.service.auth_svc.exception.CustomException;
import com.service.auth_svc.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    public RefreshToken createRefreshToken(User user, String token, Instant expiryDate) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(token)
                .user(user)
                .expiryDate(expiryDate)
                .revoked(false)
                .build();
        return refreshTokenRepository.save(refreshToken);
    }

    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new CustomException("Refresh token not found", HttpStatus.UNAUTHORIZED));

        if (rt.isRevoked()) {
            throw new CustomException("Refresh token revoked", HttpStatus.UNAUTHORIZED);
        }

        if (rt.getExpiryDate().isBefore(Instant.now())) {
            throw new CustomException("Refresh token expired", HttpStatus.UNAUTHORIZED);
        }

        return rt;
    }

    public void revokeByToken(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(rt -> {
            rt.setRevoked(true);
            refreshTokenRepository.save(rt);
        });
    }

    public void revokeAllForUser(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    public String createAndPersistRandomToken(User user, Instant expiryDate) {
        String token = UUID.randomUUID().toString();
        createRefreshToken(user, token, expiryDate);
        return token;
    }
}

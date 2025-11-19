package com.service.auth_svc.dto;

public class RevokeRequest {
    private String refreshToken;

    public RevokeRequest() {
    }

    public RevokeRequest(String refreshToken) {
        this.refreshToken = refreshToken;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public void setRefreshToken(String refreshToken) {
        this.refreshToken = refreshToken;
    }
}

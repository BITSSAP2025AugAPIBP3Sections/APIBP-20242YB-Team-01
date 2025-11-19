package com.service.auth_svc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.auth_svc.service.AuthService;
import com.service.auth_svc.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String email = token.getPrincipal().getAttribute("email");
        if (email == null) {
            // fallback to name
            email = token.getPrincipal().getName();
        }

        String role = "BUYER";
        try {
            role = authService.getRoleForEmail(email);
        } catch (Exception ignored) {
        }

        // Try to include user id in token so downstream services can use it directly
        String accessToken;
        try {
            Long userId = authService.getUserProfile(email).getId();
            accessToken = jwtService.generateAccessToken(email, role, userId);
        } catch (Exception ex) {
            accessToken = jwtService.generateAccessToken(email, role);
        }

        String opaque = null;
        try {
            opaque = authService.createOpaqueRefreshTokenForEmail(email);
        } catch (Exception ex) {
            // ignore - we'll still return access token
        }

        Map<String, String> body = new HashMap<>();
        body.put("accessToken", accessToken);
        body.put("refreshToken", opaque);

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

package com.service.auth_svc.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.service.auth_svc.service.AuthService;
import com.service.auth_svc.service.JwtService;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    private static final Logger log = LoggerFactory.getLogger(OAuth2AuthenticationSuccessHandler.class);

    private final JwtService jwtService;
    private final AuthService authService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        if (!(authentication instanceof OAuth2AuthenticationToken)) {
            log.warn("Authentication success received but not OAuth2 token: {}", authentication);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }

        OAuth2AuthenticationToken token = (OAuth2AuthenticationToken) authentication;
        String email = token.getPrincipal().getAttribute("email");

        if (email == null) {
            log.debug("Email attribute missing from OAuth2 provider, falling back to principal name");
            email = token.getPrincipal().getName();
        }

        log.info("OAuth2 login successful for email: {}", email);

        String role = "BUYER";
        try {
            role = authService.getRoleForEmail(email);
            log.debug("Resolved role '{}' for email {}", role, email);
        } catch (Exception ignored) {
            log.warn("Failed to resolve role for email {}, using default BUYER", email);
        }

        String accessToken;
        try {
            Long userId = authService.getUserProfile(email).getId();
            accessToken = jwtService.generateAccessToken(email, role, userId);
            log.info("Generated JWT access token for user '{}', role '{}', userId {}", email, role, userId);
        } catch (Exception ex) {
            accessToken = jwtService.generateAccessToken(email, role);
            log.warn("Failed to fetch userId for '{}', generated token without userId", email);
        }

        String opaque = null;
        try {
            opaque = authService.createOpaqueRefreshTokenForEmail(email);
            log.debug("Generated opaque refresh token for {}", email);
        } catch (Exception ex) {
            log.error("Failed to generate refresh token for {}, proceeding with access token only", email, ex);
        }

        Map<String, String> body = new HashMap<>();
        body.put("accessToken", accessToken);
        body.put("refreshToken", opaque);

        log.info("Returning OAuth2 login response for {}", email);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(body));
    }
}

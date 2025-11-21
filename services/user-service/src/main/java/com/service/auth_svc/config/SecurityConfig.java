package com.service.auth_svc.config;

import com.service.auth_svc.service.CustomOAuth2UserService;
import com.service.auth_svc.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    // added
    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("Initializing application security filter chain");

        http
                .csrf(csrf -> {
                    log.debug("Disabling CSRF protection (API is stateless)");
                    csrf.disable();
                })
                .cors(cors -> {
                    log.debug("Disabling CORS (handled externally)");
                    cors.disable();
                })
                .authorizeHttpRequests(auth -> {
                    log.debug("Configuring public and protected endpoints");

                    auth
                            // Allow registration and login without auth
                            .requestMatchers(HttpMethod.POST, "/api/auth/v1/register", "/api/auth/v1/login", "/api/auth/v1/refresh-token", "/api/auth/v1/revoke")
                            .permitAll()

                            // Allow oauth2 endpoints
                            .requestMatchers("/oauth2/**", "/login/**")
                            .permitAll()

                            // All other requests must be authenticated
                            .anyRequest().authenticated();
                })
                .sessionManagement(session -> {
                    log.debug("Setting session management to STATELESS");
                    session.sessionCreationPolicy(SessionCreationPolicy.STATELESS);
                })
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .oauth2Login(oauth2 -> {
                    log.info("Configuring OAuth2 login support");
                    oauth2
                            .userInfoEndpoint(userInfo -> userInfo.userService(customOAuth2UserService))
                            .successHandler(oauth2SuccessHandler);
                });

        log.info("Security filter chain built successfully");
        return http.build();
    }

    // PasswordEncoder bean moved to AppBeansConfig to avoid circular dependency

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration) throws Exception {
        log.debug("Initializing AuthenticationManager");
        return authenticationConfiguration.getAuthenticationManager();
    }
}

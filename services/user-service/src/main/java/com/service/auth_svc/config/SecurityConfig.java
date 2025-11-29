package com.service.auth_svc.config;

import com.service.auth_svc.service.CustomOAuth2UserService;
import com.service.auth_svc.service.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * Spring Security configuration for the authentication service.
 * 
 * <p>Implements a stateless JWT-based authentication architecture with support
 * for both traditional username/password login and OAuth2 social login (Google).</p>
 * 
 * <p><b>Security Architecture:</b></p>
 * <ul>
 *   <li><b>Stateless Sessions:</b> No server-side session storage, all state in JWT tokens</li>
 *   <li><b>JWT Authentication:</b> Bearer tokens validated via JwtAuthenticationFilter</li>
 *   <li><b>OAuth2 Integration:</b> Social login with Google (extensible to other providers)</li>
 *   <li><b>Method Security:</b> Supports @PreAuthorize, @PostAuthorize annotations</li>
 *   <li><b>CORS/CSRF:</b> Disabled as CORS is handled by API Gateway and CSRF is not needed for stateless APIs</li>
 * </ul>
 * 
 * <p><b>Public Endpoints:</b> Registration, login, token refresh, OAuth2 callbacks, Swagger UI</p>
 * 
 * @author APIBP Team
 * @version 1.0
 * @since 2025-11-26
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final CustomUserDetailsService customUserDetailsService;
    private final CustomOAuth2UserService customOAuth2UserService;
    private final OAuth2AuthenticationSuccessHandler oauth2SuccessHandler;

    /**
     * Configures the security filter chain with JWT and OAuth2 authentication.
     * 
     * <p>This method defines the security rules for the application:</p>
     * <ul>
     *   <li>Disables CSRF protection (not needed for stateless JWT APIs)</li>
     *   <li>Disables CORS (handled externally)</li>
     *   <li>Configures public endpoints (registration, login, OAuth2, Swagger)</li>
     *   <li>Sets session management to STATELESS</li>
     *   <li>Adds JWT authentication filter before Spring Security's default filter</li>
     *   <li>Configures OAuth2 login with custom user service and success handler</li>
     * </ul>
     * 
     * @param http HttpSecurity builder for configuring security rules
     * @return SecurityFilterChain configured security filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        log.info("Initializing application security filter chain");

        http
                .csrf(csrf -> {
                    log.debug("Disabling CSRF protection (API is stateless)");
                    csrf.disable();
                })
                .cors(cors -> {
                    log.debug("Enabling CORS to support frontend preflight requests");
                })
                .authorizeHttpRequests(auth -> {
                    log.debug("Configuring public and protected endpoints");

                    auth
                            .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()   // <-- ADDED

                            // Public endpoints: registration, login, token management
                            .requestMatchers(HttpMethod.POST, "/api/auth/v1/register", "/api/auth/v1/login",
                                    "/api/auth/v1/refresh-token", "/api/auth/v1/revoke")
                            .permitAll()

                            // Public endpoints: OAuth2 authentication flows
                            .requestMatchers("/oauth2/**", "/login/**")
                            .permitAll()

                            // Public endpoints: API documentation (Swagger/OpenAPI)
                            .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html")
                            .permitAll()

                            // Protected endpoints: all other requests require authentication
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

    /**
     * Provides the Spring Security AuthenticationManager bean.
     * 
     * <p>The AuthenticationManager is responsible for processing authentication requests.
     * It delegates to authentication providers (e.g., DaoAuthenticationProvider) to
     * validate credentials.</p>
     * 
     * <p><b>Note:</b> PasswordEncoder bean is defined in AppBeansConfig to avoid circular
     * dependency issues.</p>
     * 
     * @param authenticationConfiguration Spring Security authentication configuration
     * @return AuthenticationManager the configured authentication manager
     * @throws Exception if initialization fails
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authenticationConfiguration)
            throws Exception {
        log.debug("Initializing AuthenticationManager");
        return authenticationConfiguration.getAuthenticationManager();
    }
}

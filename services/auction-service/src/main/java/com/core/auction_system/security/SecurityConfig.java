package com.core.auction_system.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@EnableWebSecurity
@Configuration
public class SecurityConfig {

    @Value("${jwt.secret:${JWT_SECRET:}}")
    private String jwtSecret;

    @Bean
    public JwtAuthorizationFilter jwtAuthorizationFilter() {
        return new JwtAuthorizationFilter(jwtSecret);
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthorizationFilter jwtAuthorizationFilter)
            throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .authorizeHttpRequests(auth -> auth
                        // Allow products to be viewed by authenticated users (BUYER, SELLER, ADMIN)
                        .requestMatchers("/api/products/v1/**").hasAnyRole("BUYER", "SELLER", "ADMIN")
                        // Allow bids to be placed by buyers
                        .requestMatchers("/api/bids/v1/**").hasAnyRole("BUYER", "SELLER", "ADMIN")
                        // Allow categories to be viewed by authenticated users
                        .requestMatchers("/api/categories/v1/**").hasAnyRole("BUYER", "SELLER", "ADMIN")
                        // Allow Swagger/OpenAPI endpoints
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**", "/swagger-ui.html").permitAll()
                        .anyRequest().permitAll()
                )
                .addFilterBefore(jwtAuthorizationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}

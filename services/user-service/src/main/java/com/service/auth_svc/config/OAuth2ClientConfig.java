package com.service.auth_svc.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Fallback OAuth2 client configuration.
 * Provides a no-op ClientRegistrationRepository when OAuth2 is not configured.
 * This prevents application startup failures when OAuth2 credentials are not set.
 */
@Configuration
public class OAuth2ClientConfig {

    /**
     * Provides a minimal ClientRegistrationRepository when no OAuth2 properties are configured.
     * This allows the application to start even without OAuth2 credentials.
     * To enable OAuth2, configure spring.security.oauth2.client.registration properties.
     */
    @Bean
    @ConditionalOnProperty(
            prefix = "spring.security.oauth2.client.registration",
            name = "google.client-id",
            matchIfMissing = true,
            havingValue = "your-google-client-id"
    )
    public ClientRegistrationRepository clientRegistrationRepository() {
        // Return a no-op repository - OAuth2 login will not work but app will start
        return new ClientRegistrationRepository() {
            @Override
            public ClientRegistration findByRegistrationId(String registrationId) {
                return null; // No OAuth2 providers configured
            }
        };
    }
}

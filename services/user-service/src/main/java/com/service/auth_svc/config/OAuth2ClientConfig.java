package com.service.auth_svc.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;

/**
 * Provides a fallback ClientRegistrationRepository so the oauth2Login configuration
 * can start even when no external client registrations are configured.
 * <p>
 * NOTE: This registers an empty repository. To enable OAuth2 logins, add
 * spring.security.oauth2.client.registration.<provider> properties or
 * programmatically register ClientRegistration instances.
 */
@Configuration
public class OAuth2ClientConfig {

    @Bean
    public ClientRegistrationRepository clientRegistrationRepository() {
        // Return a minimal no-op implementation so the application context can start
        // when no OAuth2 client registrations are configured. Real client
        // registrations should be provided via properties or programmatically.
        return new ClientRegistrationRepository() {
            @Override
            public ClientRegistration findByRegistrationId(String registrationId) {
                return null; // no registrations available
            }
        };
    }
}

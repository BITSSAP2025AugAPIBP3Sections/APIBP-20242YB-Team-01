package com.service.auth_svc.service;

import com.service.auth_svc.entity.User;
import com.service.auth_svc.entity.UserRole;
import com.service.auth_svc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

        log.debug("OAuth2 login request via provider={}", userRequest.getClientRegistration().getRegistrationId());

        OAuth2User oauth2User = super.loadUser(userRequest);
        Map<String, Object> attributes = oauth2User.getAttributes();

        // Extract email and name from common attributes
        final String extractedEmail;
        final String extractedName;

        if (attributes.containsKey("email")) {
            extractedEmail = (String) attributes.get("email");
        } else if (attributes.containsKey("login")) { // github fallback
            extractedEmail = (String) attributes.get("login");
        } else {
            extractedEmail = null;
        }

        if (attributes.containsKey("name")) {
            extractedName = (String) attributes.get("name");
        } else if (attributes.containsKey("full_name")) {
            extractedName = (String) attributes.get("full_name");
        } else {
            extractedName = null;
        }

        log.info("OAuth2 User fetched from provider, email={}, name={}", extractedEmail, extractedName);

        // Ensure a local user exists; if not, create one with default BUYER role
        if (extractedEmail != null) {
            final String emailFinal = extractedEmail;
            final String nameFinal = extractedName;
            userRepository.findByEmail(emailFinal).orElseGet(() -> {
                log.warn("OAuth2 user not found locally, creating new user record for {}", emailFinal);
                User u = User.builder()
                        .email(emailFinal)
                        .fullName(nameFinal == null ? emailFinal : nameFinal)
                        .password("oauth2user") // placeholder; not used for oauth users
                        .enabled(true)
                        .role(UserRole.BUYER)
                        .build();
                return userRepository.save(u);
            });
        } else {
            log.error("OAuth2 provider failed to provide an email; user cannot be persisted");
        }

        return oauth2User;
    }
}

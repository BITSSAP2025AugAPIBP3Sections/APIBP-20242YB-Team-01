package com.service.auth_svc.service;

import com.service.auth_svc.entity.User;
import com.service.auth_svc.entity.UserRole;
import com.service.auth_svc.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = super.loadUser(userRequest);

        // Extract email and name from common attributes
        Map<String, Object> attributes = oauth2User.getAttributes();

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

        // Ensure a local user exists; if not, create one with default BUYER role
        if (extractedEmail != null) {
            final String emailFinal = extractedEmail;
            final String nameFinal = extractedName;
            userRepository.findByEmail(emailFinal).orElseGet(() -> {
                User u = User.builder()
                        .email(emailFinal)
                        .fullName(nameFinal == null ? emailFinal : nameFinal)
                        .password("oauth2user") // placeholder; not used for oauth users
                        .enabled(true)
                        .role(UserRole.BUYER)
                        .build();
                return userRepository.save(u);
            });
        }

        return oauth2User;
    }
}

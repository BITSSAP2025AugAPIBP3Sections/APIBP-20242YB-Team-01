package com.ebaazee.analytics_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class UserClientService {

    // added
    private static final Logger log = LoggerFactory.getLogger(UserClientService.class);

    private final WebClient webClient;
    private final String baseUrl;

    public UserClientService(@Value("${user.service.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
        log.info("UserClientService initialized with base URL: {}", baseUrl);
    }

    public String getUserName(String userId) {
        log.debug("Fetching user details for userId={}", userId);
        try {
            Mono<UserResponse> mono = webClient.get()
                    .uri("/api/v1/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserResponse.class);

            UserResponse resp = mono.block();

            if (resp != null && resp.getName() != null) {
                log.info("Fetched username '{}' for userId={}", resp.getName(), userId);
                return resp.getName();
            } else {
                log.warn("User not found or name missing for userId={}", userId);
                return "Unknown";
            }
        } catch (Exception e) {
            log.error("Failed to fetch user name for userId={} from {}", userId, baseUrl, e);
            return "Unknown";
        }
    }

    private static class UserResponse {
        private String userId;
        private String name;
        public String getName(){ return name; }
        public void setName(String name){ this.name = name; }
    }
}

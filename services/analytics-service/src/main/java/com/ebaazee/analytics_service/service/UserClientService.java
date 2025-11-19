package com.ebaazee.analytics_service.service;

import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.beans.factory.annotation.Value;
import reactor.core.publisher.Mono;

@Service
public class UserClientService {

    private final WebClient webClient;
    private final String baseUrl;

    public UserClientService(@Value("${user.service.base-url:http://localhost:8080}") String baseUrl) {
        this.baseUrl = baseUrl;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public String getUserName(String userId) {
        try {
            Mono<UserResponse> mono = webClient.get()
                    .uri("/api/v1/users/{id}", userId)
                    .retrieve()
                    .bodyToMono(UserResponse.class);
            UserResponse resp = mono.block();
            return resp != null && resp.getName() != null ? resp.getName() : "Unknown";
        } catch (Exception e) {
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

package com.core.auction_system.client;

import com.core.auction_system.dto.UserProfile;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Simple client to fetch user profile information from auth-svc.
 * Caches responses in-memory with a TTL to avoid frequent remote calls.
 */
@Component
public class UserClient {

    private static final Logger log = LoggerFactory.getLogger(UserClient.class);

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<Integer, CacheEntry> cache = new ConcurrentHashMap<>();
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;
    // TTL in seconds
    @Value("${auth.client.cache-ttl-seconds:60}")
    private long ttlSeconds;

    public UserProfile getById(Integer id) {
        if (id == null) {
            log.warn("getById called with null id");
            return null;
        }
        CacheEntry e = cache.get(id);
        if (e != null && Instant.now().isBefore(e.expiresAt)) {
            log.debug("Cache hit for user id {} (expires at {})", id, e.expiresAt);
            return e.profile;
        }
        log.debug("Cache miss for user id {}, fetching from {}", id, authServiceUrl);
        try {
            String url = authServiceUrl + "/api/auth/users/" + id;
            ResponseEntity<UserProfile> resp = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, UserProfile.class);
            UserProfile profile = resp.getBody();
            if (profile != null) {
                CacheEntry ne = new CacheEntry();
                ne.profile = profile;
                ne.expiresAt = Instant.now().plusSeconds(ttlSeconds);
                cache.put(id, ne);
                log.info("Fetched and cached user id {} until {}", id, ne.expiresAt);
            } else {
                log.warn("Received null profile from auth service for user id {}", id);
            }
            return profile;
        } catch (Exception ex) {
            log.error("Error fetching user id {} from auth service, returning cached if exists", id, ex);
            return e == null ? null : e.profile;
        }
    }

    public UserProfile getFromToken(String bearerToken) {
        if (bearerToken == null) {
            log.warn("getFromToken called with null token");
            return null;
        }
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(bearerToken.replaceFirst("Bearer\\s+", ""));
            HttpEntity<Void> ent = new HttpEntity<>(headers);
            String url = authServiceUrl + "/api/auth/me"; // expects auth-svc to support /api/auth/me which returns profile for current token
            log.debug("Fetching user profile from token using {}", url);
            ResponseEntity<UserProfile> resp = restTemplate.exchange(url, HttpMethod.GET, ent, UserProfile.class);
            log.info("Fetched profile for token user");
            return resp.getBody();
        } catch (Exception ex) {
            log.error("Error fetching profile from token", ex);
            return null;
        }
    }

    // cache entry
    private static class CacheEntry {
        UserProfile profile;
        Instant expiresAt;
    }
}

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

/**
 * Simple client to fetch user profile information from auth-svc.
 * Caches responses in-memory with a TTL to avoid frequent remote calls.
 */
@Component
public class UserClient {

    private final RestTemplate restTemplate = new RestTemplate();
    private final Map<Integer, CacheEntry> cache = new ConcurrentHashMap<>();
    @Value("${auth.service.url:http://localhost:8081}")
    private String authServiceUrl;
    // TTL in seconds
    @Value("${auth.client.cache-ttl-seconds:60}")
    private long ttlSeconds;

    public UserProfile getById(Integer id) {
        if (id == null) return null;
        CacheEntry e = cache.get(id);
        if (e != null && Instant.now().isBefore(e.expiresAt)) {
            return e.profile;
        }
        try {
            String url = authServiceUrl + "/api/auth/users/" + id;
            ResponseEntity<UserProfile> resp = restTemplate.exchange(url, HttpMethod.GET, HttpEntity.EMPTY, UserProfile.class);
            UserProfile profile = resp.getBody();
            if (profile != null) {
                CacheEntry ne = new CacheEntry();
                ne.profile = profile;
                ne.expiresAt = Instant.now().plusSeconds(ttlSeconds);
                cache.put(id, ne);
            }
            return profile;
        } catch (Exception ex) {
            // swallow and return cached if any, otherwise null
            return e == null ? null : e.profile;
        }
    }

    public UserProfile getFromToken(String bearerToken) {
        if (bearerToken == null) return null;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(bearerToken.replaceFirst("Bearer\\s+", ""));
            HttpEntity<Void> ent = new HttpEntity<>(headers);
            String url = authServiceUrl + "/api/auth/me"; // expects auth-svc to support /api/auth/me which returns profile for current token
            ResponseEntity<UserProfile> resp = restTemplate.exchange(url, HttpMethod.GET, ent, UserProfile.class);
            return resp.getBody();
        } catch (Exception ex) {
            return null;
        }
    }

    // cache entry
    private static class CacheEntry {
        UserProfile profile;
        Instant expiresAt;
    }
}

package com.service.auth_svc.controller;

import com.service.auth_svc.config.GatewayProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.HandlerMapping;

import java.util.Map;

// added
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequiredArgsConstructor
public class GatewayController {

    // added
    private static final Logger log = LoggerFactory.getLogger(GatewayController.class);

    private final GatewayProperties gatewayProperties;

    private WebClient webClientFor(String baseUrl) {
        log.debug("Creating WebClient for {}", baseUrl);
        return WebClient.builder().baseUrl(baseUrl).build();
    }

    @RequestMapping(path = "/api/{service}/**", produces = MediaType.ALL_VALUE)
    public ResponseEntity<byte[]> proxy(
            @PathVariable String service,
            @RequestHeader HttpHeaders headers,
            @RequestParam Map<String, String> params,
            @RequestBody(required = false) byte[] body,
            @RequestHeader(value = "Content-Type", required = false) String contentType,
            HttpServletRequest request
    ) {
        log.info("Gateway routing request: {} {}", request.getMethod(), request.getRequestURI());

        String targetBase = gatewayProperties.getServices().get(service);
        if (targetBase == null) {
            log.warn("Unknown service '{}' requested by client -> returning 404", service);
            return ResponseEntity.notFound().build();
        }

        log.debug("Resolved service '{}' to {}", service, targetBase);

        // compute remaining path after /api/{service}
        String bestPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        String pathWithin = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
        String remaining = "";
        if (bestPattern != null && pathWithin != null) {
            remaining = new AntPathMatcher().extractPathWithinPattern(bestPattern, pathWithin);
        }

        StringBuilder uriBuilder = new StringBuilder();
        uriBuilder.append(targetBase);
        if (remaining != null && !remaining.isEmpty()) {
            if (!remaining.startsWith("/")) uriBuilder.append('/');
            uriBuilder.append(remaining);
        }
        if (!params.isEmpty()) {
            uriBuilder.append('?');
            params.forEach((k, v) -> uriBuilder.append(k).append('=').append(v).append('&'));
            uriBuilder.setLength(uriBuilder.length() - 1);
        }

        String uri = uriBuilder.toString();
        log.info("Proxying to upstream URI: {}", uri);

        WebClient client = webClientFor(targetBase);

        HttpMethod httpMethod;
        try {
            httpMethod = HttpMethod.valueOf(request.getMethod());
        } catch (IllegalArgumentException e) {
            log.warn("Unsupported HTTP method '{}' -> defaulting to GET", request.getMethod());
            httpMethod = HttpMethod.GET;
        }

        WebClient.RequestBodySpec reqSpec = client.method(httpMethod).uri(uri).headers(h -> {
            headers.forEach((k, v) -> {
                if (!k.equalsIgnoreCase(HttpHeaders.HOST)) {
                    h.put(k, v);
                }
            });
        });

        WebClient.ResponseSpec responseSpec;
        if (body != null && body.length > 0) {
            log.debug("Forwarding request with body ({} bytes)", body.length);
            responseSpec = reqSpec.contentType(contentType != null ? MediaType.parseMediaType(contentType) : MediaType.APPLICATION_OCTET_STREAM)
                    .body(BodyInserters.fromValue(body))
                    .retrieve();
        } else {
            log.debug("Forwarding request without body");
            responseSpec = reqSpec.retrieve();
        }

        try {
            ResponseEntity<byte[]> resp = responseSpec.toEntity(byte[].class).block();
            if (resp == null) {
                log.error("Upstream {} returned empty response", service);
                return ResponseEntity.status(502).body(("Upstream returned empty response").getBytes());
            }
            log.info("Upstream response status {} from {}", resp.getStatusCode(), service);
            return ResponseEntity.status(resp.getStatusCode()).headers(resp.getHeaders()).body(resp.getBody());
        } catch (Exception ex) {
            log.error("Upstream error for service {}: {}", service, ex.getMessage());
            return ResponseEntity.status(502).body(("Upstream error: " + ex.getMessage()).getBytes());
        }
    }
}

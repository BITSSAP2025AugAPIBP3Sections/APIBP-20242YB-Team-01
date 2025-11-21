package com.service.auth_svc.config;

import java.util.Map;
import java.util.Collections;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {
    private Map<String, String> services;

    public Map<String, String> getServices() {
        return services == null ? Collections.emptyMap() : services;
    }

    public void setServices(Map<String, String> services) {
        this.services = services;
    }
}

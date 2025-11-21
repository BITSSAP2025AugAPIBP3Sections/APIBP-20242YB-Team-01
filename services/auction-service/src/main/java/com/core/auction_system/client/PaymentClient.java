package com.core.auction_system.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
public class PaymentClient {

    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${payment.service.url:http://payment-service:8081}")
    private String paymentServiceUrl;

    public static class FreezeResponse {
        public boolean ok;
        public String reservationId;
        public String reason;
    }

    public FreezeResponse freeze(Integer userId, Double amount, String email) {
        String url = paymentServiceUrl + "/wallet/freeze";
        HttpHeaders headers = new HttpHeaders();
        Map<String, Object> body = Map.of("userId", userId.toString(), "amount", amount, "email", email);
        HttpEntity<Map<String, Object>> ent = new HttpEntity<>(body, headers);
        try {
            ResponseEntity<FreezeResponse> resp = rest.exchange(url, HttpMethod.POST, ent, FreezeResponse.class);
            return resp.getBody();
        } catch (HttpStatusCodeException ex) {
            // Try to parse JSON body { ok:false, reason: "..." } to return a clean reason
            FreezeResponse fr = new FreezeResponse();
            fr.ok = false;
            try {
                String respBody = ex.getResponseBodyAsString();
                if (respBody != null && !respBody.isBlank()) {
                    JsonNode node = mapper.readTree(respBody);
                    if (node.has("reason")) {
                        fr.reason = node.get("reason").asText();
                    } else if (node.has("message")) {
                        fr.reason = node.get("message").asText();
                    } else {
                        fr.reason = respBody;
                    }
                } else {
                    fr.reason = ex.getStatusCode().toString();
                }
            } catch (Exception parseEx) {
                fr.reason = ex.getStatusCode().toString() + " - " + ex.getMessage();
            }
            return fr;
        } catch (Exception ex) {
            FreezeResponse fr = new FreezeResponse();
            fr.ok = false;
            fr.reason = ex.getMessage();
            return fr;
        }
    }
}


package com.befapress.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmsService {

    private final RestTemplate restTemplate = new RestTemplate();

    // Configuration - should be in application.properties in production
    private static final String API_KEY = "02944179-2829-4f0c-85bc-84cecbce9e0f";
    private static final String DEVICE_ID = "695c121dd6a8a5e247788ab4";
    private static final String TEXTBEE_URL = "https://api.textbee.dev/api/v1/gateway/devices/" + DEVICE_ID
            + "/send-sms";

    public void sendSms(String to, String message) {
        try {
            log.info("Sending SMS to {} via TextBee", to);

            HttpHeaders headers = new HttpHeaders();
            headers.set("x-api-key", API_KEY);
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = new HashMap<>();
            body.put("recipients", Collections.singletonList(to));
            body.put("message", message);

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(TEXTBEE_URL, request, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("SMS sent successfully: {}", response.getBody());
            } else {
                log.error("Failed to send SMS. Status: {}, Body: {}", response.getStatusCode(), response.getBody());
                // Don't throw exception to avoid blocking registration if SMS fails (optional)
                throw new RuntimeException("Failed to send SMS via TextBee");
            }

        } catch (Exception e) {
            log.error("Error sending SMS", e);
            throw new RuntimeException("Error sending SMS: " + e.getMessage());
        }
    }
}

package com.befapress.service;

import com.befapress.config.TelebirrConfig;
import com.befapress.util.TelebirrSignatureUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.*;
import java.security.cert.X509Certificate;
import java.util.*;

/**
 * Service for Telebirr payment integration.
 * Handles token acquisition, order creation, and checkout URL generation.
 */
@Service
public class TelebirrService {

    private static final Logger logger = LoggerFactory.getLogger(TelebirrService.class);
    private final TelebirrConfig config;
    private final TelebirrSignatureUtil signatureUtil;
    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate;

    // Cache for fabric token
    private String cachedToken;
    private long tokenExpirationTime;

    @Autowired
    public TelebirrService(TelebirrConfig config, TelebirrSignatureUtil signatureUtil) {
        this.config = config;
        this.signatureUtil = signatureUtil;
        this.objectMapper = new ObjectMapper();
        this.restTemplate = createTrustAllRestTemplate();
    }

    /**
     * Gets a fabric token, using cache if valid.
     */
    public String getFabricToken() {
        if (cachedToken != null && System.currentTimeMillis() < tokenExpirationTime) {
            return cachedToken;
        }

        try {
            String url = config.getBaseUrl() + "/payment/v1/token";
            logger.info("Requesting fabric token from: {}", url);
            logger.info("Using X-APP-Key: {}", config.getFabricAppId());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("X-APP-Key", config.getFabricAppId());

            Map<String, String> body = new HashMap<>();
            body.put("appSecret", config.getAppSecret());

            HttpEntity<Map<String, String>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            logger.info("Fabric token response status: {}", response.getStatusCode());
            logger.debug("Fabric token response body: {}", response.getBody());

            JsonNode jsonNode = objectMapper.readTree(response.getBody());
            cachedToken = jsonNode.get("token").asText();

            // Token valid for 1 hour, cache for 50 minutes to be safe
            tokenExpirationTime = System.currentTimeMillis() + (50 * 60 * 1000);

            logger.info("Obtained new Telebirr fabric token successfully");
            return cachedToken;
        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.error("Network error connecting to Telebirr: {} - {}", e.getMessage(),
                    e.getCause() != null ? e.getCause().getMessage() : "No cause");
            throw new RuntimeException("Network error connecting to Telebirr: " + e.getMessage(), e);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            logger.error("Telebirr returned error: {} - {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new RuntimeException("Telebirr API error: " + e.getStatusCode() + " - " + e.getResponseBodyAsString(),
                    e);
        } catch (Exception e) {
            logger.error("Failed to get fabric token: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to obtain Telebirr fabric token: " + e.getMessage(), e);
        }
    }

    /**
     * Creates a payment order and returns the checkout URL.
     * 
     * @param amount The payment amount in ETB
     * @param title  The payment description/title
     * @return The checkout URL to redirect the user to
     */
    public String createOrderAndGetCheckoutUrl(String amount, String title) {
        try {
            String fabricToken = getFabricToken();
            String prepayId = createOrder(fabricToken, amount, title);
            return generateCheckoutUrl(prepayId);
        } catch (Exception e) {
            logger.error("Failed to create order: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to create Telebirr payment order", e);
        }
    }

    /**
     * Creates a pre-order and returns the prepay_id.
     */
    private String createOrder(String fabricToken, String amount, String title) throws Exception {
        String url = config.getBaseUrl() + "/payment/v1/merchant/preOrder";

        // Build request object
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("timestamp", signatureUtil.generateTimestamp());
        request.put("nonce_str", signatureUtil.generateNonceStr());
        request.put("method", "payment.preorder");
        request.put("version", "1.0");

        // Build biz_content
        Map<String, Object> bizContent = new LinkedHashMap<>();
        bizContent.put("notify_url", config.getNotifyUrl());
        bizContent.put("appid", config.getMerchantAppId());
        bizContent.put("merch_code", config.getMerchantCode());
        bizContent.put("merch_order_id", String.valueOf(System.currentTimeMillis()));
        bizContent.put("trade_type", "Checkout");
        bizContent.put("title", title);
        bizContent.put("total_amount", amount);
        bizContent.put("trans_currency", "ETB");
        bizContent.put("timeout_express", "120m");
        bizContent.put("business_type", "BuyGoods");
        bizContent.put("payee_identifier", config.getMerchantCode());
        bizContent.put("payee_identifier_type", "04");
        bizContent.put("payee_type", "5000");

        if (config.getRedirectUrl() != null && !config.getRedirectUrl().isEmpty()) {
            bizContent.put("redirect_url", config.getRedirectUrl());
        }

        request.put("biz_content", bizContent);

        // Sign the request
        String sign = signatureUtil.signRequestObject(request, config.getPrivateKey());
        request.put("sign", sign);
        request.put("sign_type", "SHA256WithRSA");

        // Send request
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-APP-Key", config.getFabricAppId());
        headers.set("Authorization", fabricToken);

        HttpEntity<Map<String, Object>> httpRequest = new HttpEntity<>(request, headers);
        ResponseEntity<String> response = restTemplate.postForEntity(url, httpRequest, String.class);

        logger.info("Create order response: {}", response.getBody());

        JsonNode jsonNode = objectMapper.readTree(response.getBody());
        if (!"SUCCESS".equals(jsonNode.get("result").asText())) {
            throw new RuntimeException("Order creation failed: " + jsonNode.get("msg").asText());
        }

        return jsonNode.get("biz_content").get("prepay_id").asText();
    }

    /**
     * Generates the checkout URL from a prepay_id.
     */
    private String generateCheckoutUrl(String prepayId) {
        Map<String, String> params = new TreeMap<>();
        params.put("appid", config.getMerchantAppId());
        params.put("merch_code", config.getMerchantCode());
        params.put("nonce_str", signatureUtil.generateNonceStr());
        params.put("prepay_id", prepayId);
        params.put("timestamp", signatureUtil.generateTimestamp());

        // Build string to sign
        StringBuilder signBuilder = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (signBuilder.length() > 0) {
                signBuilder.append("&");
            }
            signBuilder.append(entry.getKey()).append("=").append(entry.getValue());
        }

        String sign = signatureUtil.signString(signBuilder.toString(), config.getPrivateKey());
        params.put("sign", sign);
        params.put("sign_type", "SHA256WithRSA");

        // Build URL
        StringBuilder urlBuilder = new StringBuilder(config.getWebCheckoutUrl());
        boolean first = true;
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (!first) {
                urlBuilder.append("&");
            }
            first = false;
            urlBuilder.append(entry.getKey()).append("=").append(entry.getValue());
        }
        urlBuilder.append("&version=1.0&trade_type=Checkout");

        return urlBuilder.toString();
    }

    /**
     * Creates a RestTemplate that trusts all SSL certificates.
     * Required for Telebirr's self-signed certs in test environment.
     */
    private RestTemplate createTrustAllRestTemplate() {
        try {
            // Create a trust manager that trusts all certificates
            TrustManager[] trustAllCerts = new TrustManager[] {
                    new X509TrustManager() {
                        public X509Certificate[] getAcceptedIssuers() {
                            return new X509Certificate[0];
                        }

                        public void checkClientTrusted(X509Certificate[] certs, String authType) {
                        }

                        public void checkServerTrusted(X509Certificate[] certs, String authType) {
                        }
                    }
            };

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustAllCerts, new java.security.SecureRandom());

            // Set the default SSL socket factory and hostname verifier globally
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier((hostname, session) -> true);

            // Create a custom request factory that uses our SSL context
            javax.net.ssl.HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory());

            org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory() {
                @Override
                protected void prepareConnection(java.net.HttpURLConnection connection, String httpMethod)
                        throws java.io.IOException {
                    if (connection instanceof HttpsURLConnection) {
                        ((HttpsURLConnection) connection).setSSLSocketFactory(sslContext.getSocketFactory());
                        ((HttpsURLConnection) connection).setHostnameVerifier((hostname, session) -> true);
                    }
                    super.prepareConnection(connection, httpMethod);
                }
            };

            factory.setConnectTimeout(30000);
            factory.setReadTimeout(30000);

            return new RestTemplate(factory);
        } catch (Exception e) {
            logger.warn("Failed to configure trust-all SSL, using default RestTemplate", e);
            return new RestTemplate();
        }
    }
}

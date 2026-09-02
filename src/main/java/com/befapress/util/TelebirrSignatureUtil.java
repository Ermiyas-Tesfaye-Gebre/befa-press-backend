package com.befapress.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.*;

/**
 * Utility class for Telebirr RSA signing operations.
 * Follows the Telebirr API signature requirements (SHA256WithRSA).
 */
@Component
public class TelebirrSignatureUtil {

    private static final Logger logger = LoggerFactory.getLogger(TelebirrSignatureUtil.class);
    private static final String ALGORITHM = "SHA256withRSA";
    private static final Set<String> EXCLUDED_FIELDS = Set.of("sign", "sign_type", "header", "refund_info", "openType",
            "raw_request", "biz_content");

    /**
     * Signs the request object using RSA SHA256.
     * 
     * @param requestMap       The request parameters (including biz_content as a
     *                         nested Map)
     * @param privateKeyBase64 The RSA private key in Base64 format
     * @return The signature string
     */
    public String signRequestObject(Map<String, Object> requestMap, String privateKeyBase64) {
        try {
            // Flatten the map (extract biz_content fields)
            Map<String, String> flatMap = new TreeMap<>();

            for (Map.Entry<String, Object> entry : requestMap.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();

                if (EXCLUDED_FIELDS.contains(key)) {
                    // If it's biz_content, extract its fields
                    if ("biz_content".equals(key) && value instanceof Map) {
                        @SuppressWarnings("unchecked")
                        Map<String, Object> bizContent = (Map<String, Object>) value;
                        for (Map.Entry<String, Object> bizEntry : bizContent.entrySet()) {
                            if (!EXCLUDED_FIELDS.contains(bizEntry.getKey())) {
                                flatMap.put(bizEntry.getKey(), String.valueOf(bizEntry.getValue()));
                            }
                        }
                    }
                    continue;
                }

                flatMap.put(key, String.valueOf(value));
            }

            // Build the string to sign (alphabetically sorted, key=value, joined with &)
            StringBuilder signBuilder = new StringBuilder();
            for (Map.Entry<String, String> entry : flatMap.entrySet()) {
                if (signBuilder.length() > 0) {
                    signBuilder.append("&");
                }
                signBuilder.append(entry.getKey()).append("=").append(entry.getValue());
            }

            String stringToSign = signBuilder.toString();
            logger.debug("String to sign: {}", stringToSign);

            return signString(stringToSign, privateKeyBase64);
        } catch (Exception e) {
            logger.error("Error signing request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sign Telebirr request", e);
        }
    }

    /**
     * Signs a string using RSA SHA256.
     */
    public String signString(String text, String privateKeyBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(privateKeyBase64);
            PKCS8EncodedKeySpec keySpec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PrivateKey privateKey = keyFactory.generatePrivate(keySpec);

            Signature signature = Signature.getInstance(ALGORITHM);
            signature.initSign(privateKey);
            signature.update(text.getBytes(StandardCharsets.UTF_8));

            byte[] signedBytes = signature.sign();
            return Base64.getEncoder().encodeToString(signedBytes);
        } catch (Exception e) {
            logger.error("Error signing string: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to sign string", e);
        }
    }

    /**
     * Generates a random nonce string (32 characters).
     */
    public String generateNonceStr() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(32);
        Random random = new Random();
        for (int i = 0; i < 32; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }

    /**
     * Generates a timestamp in seconds.
     */
    public String generateTimestamp() {
        return String.valueOf(System.currentTimeMillis() / 1000);
    }
}

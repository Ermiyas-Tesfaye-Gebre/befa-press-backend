package com.befapress.controller;

import com.befapress.dto.request.PaymentRequest;
import com.befapress.service.TelebirrService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Controller for payment operations (Telebirr integration).
 */
@RestController
@RequestMapping("/api/payment")
@CrossOrigin(origins = "*")
public class PaymentController {

    private static final Logger logger = LoggerFactory.getLogger(PaymentController.class);
    private final TelebirrService telebirrService;

    public PaymentController(TelebirrService telebirrService) {
        this.telebirrService = telebirrService;
    }

    /**
     * Initiates a donation payment and returns the Telebirr checkout URL.
     * 
     * @param request The payment request containing amount and title
     * @return A response containing the checkout URL
     */
    @PostMapping("/donate")
    public ResponseEntity<Map<String, String>> initiateDonation(@Valid @RequestBody PaymentRequest request) {
        logger.info("Initiating donation: amount={}, title={}", request.getAmount(), request.getTitle());

        try {
            String checkoutUrl = telebirrService.createOrderAndGetCheckoutUrl(
                    String.valueOf(request.getAmount()),
                    request.getTitle());

            Map<String, String> response = new HashMap<>();
            response.put("checkoutUrl", checkoutUrl);
            response.put("status", "success");

            logger.info("Donation initiated successfully, redirecting to checkout");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Failed to initiate donation: {}", e.getMessage(), e);

            String errorMessage = e.getMessage();
            if (e.getCause() != null) {
                errorMessage += " Cause: " + e.getCause().getMessage();
            }

            Map<String, String> response = new HashMap<>();
            response.put("status", "error");
            response.put("message", "Failed to initiate payment: " + errorMessage);

            return ResponseEntity.badRequest().body(response);
        }
    }

    /**
     * Callback endpoint for Telebirr to notify us of payment status.
     * This endpoint must be whitelisted on Telebirr's server.
     */
    @PostMapping("/callback")
    public ResponseEntity<Map<String, String>> handlePaymentCallback(@RequestBody Map<String, Object> payload) {
        logger.info("Received payment callback: {}", payload);

        try {
            String tradeStatus = (String) payload.get("trade_status");
            String merchOrderId = (String) payload.get("merch_order_id");
            String totalAmount = (String) payload.get("total_amount");
            String transId = (String) payload.get("trans_id");

            if ("Completed".equals(tradeStatus)) {
                logger.info("Payment completed: orderId={}, amount={}, transId={}",
                        merchOrderId, totalAmount, transId);
                // TODO: Save donation to database, send thank you email, etc.
            } else {
                logger.warn("Payment not completed: status={}, orderId={}", tradeStatus, merchOrderId);
            }

            Map<String, String> response = new HashMap<>();
            response.put("result", "SUCCESS");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error processing payment callback: {}", e.getMessage(), e);

            Map<String, String> response = new HashMap<>();
            response.put("result", "FAIL");
            return ResponseEntity.ok(response);
        }
    }

    /**
     * Endpoint to check Telebirr configuration status (for debugging).
     */
    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getPaymentStatus() {
        Map<String, Object> response = new HashMap<>();
        response.put("telebirrEnabled", true);
        response.put("provider", "Telebirr");
        response.put("environment", "testbed");
        return ResponseEntity.ok(response);
    }
}

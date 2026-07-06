package com.corc.backend.controller;

import com.corc.backend.dto.response.ApiResponse;
import com.corc.backend.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/webhook")
@RequiredArgsConstructor
public class WebhookController {

    private final PaymentService paymentService;

    @PostMapping("/stripe")
    public ResponseEntity<ApiResponse<Void>> handleStripeWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "Stripe-Signature", required = false) String signature) {

        paymentService.verifyWebhookSignature("stripe", payload.toString(), signature);
        paymentService.handleWebhookEvent("stripe", payload);
        return ResponseEntity.ok(ApiResponse.ok("Webhook processed", null));
    }

    @PostMapping("/razorpay")
    public ResponseEntity<ApiResponse<Void>> handleRazorpayWebhook(
            @RequestBody Map<String, Object> payload,
            @RequestHeader(value = "X-Razorpay-Signature", required = false) String signature) {

        paymentService.verifyWebhookSignature("razorpay", payload.toString(), signature);
        paymentService.handleWebhookEvent("razorpay", payload);
        return ResponseEntity.ok(ApiResponse.ok("Webhook processed", null));
    }
}

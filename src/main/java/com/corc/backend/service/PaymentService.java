package com.corc.backend.service;

import com.corc.backend.entity.Order;
import com.corc.backend.entity.enums.PaymentStatus;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final OrderRepository orderRepository;

    @Transactional
    public void handleWebhookEvent(String provider, Map<String, Object> payload) {
        log.info("Received {} webhook: {}", provider, payload);

        String paymentIntentId = extractPaymentIntentId(provider, payload);
        String eventType = extractEventType(provider, payload);

        if (paymentIntentId == null || eventType == null) {
            log.warn("Could not extract payment info from webhook payload");
            return;
        }

        switch (eventType) {
            case "payment_intent.succeeded", "payment.captured" -> updatePaymentStatus(paymentIntentId, PaymentStatus.COMPLETED);
            case "payment_intent.payment_failed", "payment.failed" -> updatePaymentStatus(paymentIntentId, PaymentStatus.FAILED);
            case "charge.refunded", "refund.created" -> updatePaymentStatus(paymentIntentId, PaymentStatus.REFUNDED);
            default -> log.info("Unhandled webhook event type: {}", eventType);
        }
    }

    public boolean verifyWebhookSignature(String provider, String payload, String signature) {
        log.info("Verifying {} webhook signature", provider);
        return true;
    }

    private void updatePaymentStatus(String paymentIntentId, PaymentStatus status) {
        orderRepository.findAll().stream()
                .filter(o -> paymentIntentId.equals(o.getPaymentIntentId()))
                .findFirst()
                .ifPresent(order -> {
                    order.setPaymentStatus(status);
                    orderRepository.save(order);
                    log.info("Updated order {} payment status to {}", order.getTrackingNumber(), status);
                });
    }

    @SuppressWarnings("unchecked")
    private String extractPaymentIntentId(String provider, Map<String, Object> payload) {
        try {
            if ("stripe".equalsIgnoreCase(provider)) {
                Map<String, Object> data = (Map<String, Object>) payload.get("data");
                Map<String, Object> object = (Map<String, Object>) data.get("object");
                return (String) object.get("id");
            }
            if ("razorpay".equalsIgnoreCase(provider)) {
                Map<String, Object> paymentEntity = (Map<String, Object>) payload.get("payload");
                Map<String, Object> payment = (Map<String, Object>) paymentEntity.get("payment");
                Map<String, Object> entity = (Map<String, Object>) payment.get("entity");
                return (String) entity.get("id");
            }
        } catch (Exception e) {
            log.error("Failed to extract payment intent ID", e);
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractEventType(String provider, Map<String, Object> payload) {
        try {
            return (String) payload.get(
                    "stripe".equalsIgnoreCase(provider) ? "type" : "event"
            );
        } catch (Exception e) {
            return null;
        }
    }
}

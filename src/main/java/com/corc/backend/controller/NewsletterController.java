package com.corc.backend.controller;

import com.corc.backend.dto.response.ApiResponse;
import com.corc.backend.service.NewsletterService;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/newsletter")
@RequiredArgsConstructor
public class NewsletterController {

    private final NewsletterService newsletterService;

    @PostMapping("/subscribe")
    public ResponseEntity<ApiResponse<Void>> subscribe(@RequestBody Map<String, @NotBlank @Email String> body) {
        String email = body.get("email");
        if (email == null || email.isBlank() || !email.contains("@")) {
            return ResponseEntity.badRequest().body(ApiResponse.error("Valid email is required"));
        }
        newsletterService.subscribe(email);
        return ResponseEntity.ok(ApiResponse.ok("Subscribed successfully", null));
    }

    @PostMapping("/unsubscribe")
    public ResponseEntity<ApiResponse<Void>> unsubscribe(@RequestBody Map<String, String> body) {
        newsletterService.unsubscribe(body.get("email"));
        return ResponseEntity.ok(ApiResponse.ok("Unsubscribed", null));
    }
}

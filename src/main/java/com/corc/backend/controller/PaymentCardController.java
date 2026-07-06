package com.corc.backend.controller;

import com.corc.backend.dto.request.PaymentCardRequest;
import com.corc.backend.dto.response.ApiResponse;
import com.corc.backend.service.PaymentCardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/cards")
@RequiredArgsConstructor
public class PaymentCardController {

    private final PaymentCardService cardService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getCards(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(cardService.getCards(userDetails.getUsername())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> addCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody PaymentCardRequest request) {
        Map<String, Object> card = cardService.addCard(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Card Saved", card));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> removeCard(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        cardService.removeCard(userDetails.getUsername(), id);
        return ResponseEntity.ok(ApiResponse.ok("Card Removed", null));
    }
}

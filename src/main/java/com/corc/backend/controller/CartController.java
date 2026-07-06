package com.corc.backend.controller;

import com.corc.backend.dto.request.CartItemRequest;
import com.corc.backend.dto.request.CartUpdateRequest;
import com.corc.backend.dto.response.ApiResponse;
import com.corc.backend.dto.response.CartItemResponse;
import com.corc.backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<CartItemResponse>>> getCart(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(ApiResponse.ok(cartService.getCart(userDetails.getUsername())));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<CartItemResponse>> addToCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody CartItemRequest request) {
        CartItemResponse item = cartService.addToCart(userDetails.getUsername(), request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Added to Cart", item));
    }

    @PatchMapping("/{uniqueKey}")
    public ResponseEntity<ApiResponse<CartItemResponse>> updateQuantity(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uniqueKey,
            @Valid @RequestBody CartUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(
                cartService.updateQuantity(userDetails.getUsername(), uniqueKey, request.getQuantity())));
    }

    @DeleteMapping("/{uniqueKey}")
    public ResponseEntity<ApiResponse<Void>> removeFromCart(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable String uniqueKey) {
        cartService.removeFromCart(userDetails.getUsername(), uniqueKey);
        return ResponseEntity.ok(ApiResponse.ok("Removed from cart", null));
    }
}

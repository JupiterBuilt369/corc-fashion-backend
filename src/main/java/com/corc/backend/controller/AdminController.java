package com.corc.backend.controller;

import com.corc.backend.dto.request.ProductCreateRequest;
import com.corc.backend.dto.request.StockAdjustmentRequest;
import com.corc.backend.dto.response.ApiResponse;
import com.corc.backend.dto.response.ProductResponse;
import com.corc.backend.service.InventoryService;
import com.corc.backend.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final InventoryService inventoryService;
    private final ProductService productService;

    @GetMapping("/inventory/low-stock")
    public ResponseEntity<ApiResponse<List<Map<String, Object>>>> getLowStockProducts() {
        return ResponseEntity.ok(ApiResponse.ok(inventoryService.getLowStockProducts()));
    }

    @PostMapping("/inventory/adjust")
    public ResponseEntity<ApiResponse<Map<String, Object>>> adjustStock(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody StockAdjustmentRequest request) {
        Map<String, Object> result = inventoryService.adjustStock(userDetails.getUsername(), request);
        return ResponseEntity.ok(ApiResponse.ok("Stock adjusted", result));
    }

    @PostMapping("/products")
    public ResponseEntity<ApiResponse<ProductResponse>> createProduct(
            @Valid @RequestBody ProductCreateRequest request) {
        ProductResponse product = productService.createProduct(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Product Created", product));
    }

    @DeleteMapping("/products/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable Long id) {
        productService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.ok("Product Deleted", null));
    }
}

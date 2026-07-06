package com.corc.backend.service;

import com.corc.backend.dto.request.StockAdjustmentRequest;
import com.corc.backend.dto.response.ProductResponse;
import com.corc.backend.entity.Product;
import com.corc.backend.entity.StockAdjustmentLog;
import com.corc.backend.entity.User;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.ProductRepository;
import com.corc.backend.repository.StockAdjustmentLogRepository;
import com.corc.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class InventoryService {

    private final ProductRepository productRepository;
    private final StockAdjustmentLogRepository stockLogRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<Map<String, Object>> getLowStockProducts() {
        return productRepository.findLowStockProducts().stream()
                .map(p -> Map.<String, Object>of(
                        "id", p.getId(),
                        "name", p.getName(),
                        "category", p.getCategory(),
                        "stockQuantity", p.getStockQuantity(),
                        "lowStockThreshold", p.getLowStockThreshold()
                ))
                .toList();
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public Map<String, Object> adjustStock(String adminEmail, StockAdjustmentRequest request) {
        User admin = userRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", adminEmail));

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        int previousQty = product.getStockQuantity();
        int newQty = previousQty + request.getQuantityChange();
        if (newQty < 0) newQty = 0;

        product.setStockQuantity(newQty);
        productRepository.save(product);

        StockAdjustmentLog log = StockAdjustmentLog.builder()
                .product(product)
                .quantityChanged(request.getQuantityChange())
                .previousQuantity(previousQty)
                .newQuantity(newQty)
                .reason(request.getReason())
                .notes(request.getNotes())
                .adjustedBy(admin)
                .build();

        stockLogRepository.save(log);

        return Map.of(
                "productId", product.getId(),
                "productName", product.getName(),
                "previousQuantity", previousQty,
                "newQuantity", newQty,
                "adjustment", request.getQuantityChange(),
                "reason", request.getReason().name()
        );
    }
}

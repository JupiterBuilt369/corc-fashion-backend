package com.corc.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long id;
    private String uniqueKey;
    private Long productId;
    private String name;
    private BigDecimal price;
    private String image;
    private String category;
    private String size;
    private int quantity;
    private String customDesign;
    private String color;
}

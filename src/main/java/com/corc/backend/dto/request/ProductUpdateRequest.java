package com.corc.backend.dto.request;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductUpdateRequest {
    private String name;
    private String category;
    private String description;
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    private Integer stock;
    private String sku;
    private Boolean featured;
    private Boolean active;
    private List<String> imageUrls;
}

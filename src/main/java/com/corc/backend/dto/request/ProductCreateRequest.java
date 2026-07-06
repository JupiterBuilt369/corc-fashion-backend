package com.corc.backend.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class ProductCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String category;
    private String description;
    @NotNull @DecimalMin("0.01")
    private BigDecimal price;
    private BigDecimal compareAtPrice;
    @NotNull @Min(0)
    private Integer stock;
    private String sku;
    private boolean featured;
    private List<String> imageUrls;
}

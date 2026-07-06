package com.corc.backend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CartItemRequest {
    @NotNull
    private Long productId;
    @NotBlank
    private String size;
    @Min(1)
    private int quantity = 1;
    private String customDesign;
    private String color;
}

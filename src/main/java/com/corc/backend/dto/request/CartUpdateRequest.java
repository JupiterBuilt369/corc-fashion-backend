package com.corc.backend.dto.request;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class CartUpdateRequest {
    @Min(1)
    private int quantity;
}

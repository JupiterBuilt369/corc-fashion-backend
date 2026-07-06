package com.corc.backend.dto.request;

import com.corc.backend.entity.enums.StockAdjustmentReason;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StockAdjustmentRequest {
    @NotNull
    private Long productId;
    @NotNull
    private Integer quantityChange;
    @NotNull
    private StockAdjustmentReason reason;
    private String notes;
}

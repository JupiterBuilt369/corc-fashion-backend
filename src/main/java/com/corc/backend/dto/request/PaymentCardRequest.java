package com.corc.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class PaymentCardRequest {
    @NotBlank
    private String type;
    @NotBlank @Size(min = 4, max = 4)
    private String last4;
    @NotBlank
    private String expiry;
    private boolean isDefault;
}

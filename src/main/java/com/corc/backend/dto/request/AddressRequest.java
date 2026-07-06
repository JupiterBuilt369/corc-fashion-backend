package com.corc.backend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AddressRequest {
    @NotBlank
    private String type;
    @NotBlank
    private String street;
    @NotBlank
    private String city;
    @NotBlank
    private String zip;
    private String country;
    private boolean isDefault;
}

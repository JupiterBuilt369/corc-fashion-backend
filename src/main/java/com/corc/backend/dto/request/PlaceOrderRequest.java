package com.corc.backend.dto.request;

import lombok.Data;

@Data
public class PlaceOrderRequest {
    private String shippingAddress;
    private String couponCode;
    private String idempotencyKey;
}

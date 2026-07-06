package com.corc.backend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ReviewRequest {
    @NotNull
    private Long productId;
    @Min(1) @Max(5)
    private int rating;
    private String text;
    private String image;
}

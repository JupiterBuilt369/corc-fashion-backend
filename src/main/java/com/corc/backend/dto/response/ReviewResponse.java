package com.corc.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long productId;
    private String userName;
    private String userAvatar;
    private int rating;
    private String text;
    private String image;
    private String date;
    private String createdAt;
}

package com.corc.backend.service;

import com.corc.backend.dto.request.ReviewRequest;
import com.corc.backend.dto.response.ReviewResponse;
import com.corc.backend.entity.Product;
import com.corc.backend.entity.ProductReview;
import com.corc.backend.entity.User;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.ProductRepository;
import com.corc.backend.repository.ProductReviewRepository;
import com.corc.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.ENGLISH);

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(Long productId) {
        return reviewRepository.findByProductIdOrderByCreatedAtDesc(productId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getAllReviews() {
        return reviewRepository.findAll().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public ReviewResponse createReview(String email, ReviewRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .rating(request.getRating())
                .text(request.getText())
                .image(request.getImage())
                .build();

        review = reviewRepository.save(review);
        return mapToResponse(review);
    }

    private ReviewResponse mapToResponse(ProductReview review) {
        String displayDate = review.getCreatedAt() != null
                ? review.getCreatedAt().atOffset(ZoneOffset.UTC).format(DISPLAY_FORMAT)
                : "";

        return ReviewResponse.builder()
                .id(review.getId())
                .productId(review.getProduct().getId())
                .userName(review.getUser().getName())
                .userAvatar(review.getUser().getAvatar())
                .rating(review.getRating())
                .text(review.getText())
                .image(review.getImage())
                .date(displayDate)
                .createdAt(review.getCreatedAt() != null ? review.getCreatedAt().toString() : null)
                .build();
    }
}

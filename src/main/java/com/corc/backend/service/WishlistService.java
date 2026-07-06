package com.corc.backend.service;

import com.corc.backend.dto.response.ProductResponse;
import com.corc.backend.entity.Product;
import com.corc.backend.entity.User;
import com.corc.backend.entity.WishlistItem;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.ProductRepository;
import com.corc.backend.repository.UserRepository;
import com.corc.backend.repository.WishlistItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistItemRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> getWishlist(String email) {
        User user = findUser(email);
        return wishlistRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(item -> mapProductToResponse(item.getProduct()))
                .toList();
    }

    @Transactional
    public void toggleWishlist(String email, Long productId) {
        User user = findUser(email);

        if (wishlistRepository.existsByUserIdAndProductId(user.getId(), productId)) {
            WishlistItem item = wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                    .orElseThrow();
            wishlistRepository.delete(item);
        } else {
            Product product = productRepository.findById(productId)
                    .orElseThrow(() -> new ResourceNotFoundException("Product", "id", productId));
            WishlistItem item = WishlistItem.builder()
                    .user(user)
                    .product(product)
                    .build();
            wishlistRepository.save(item);
        }
    }

    @Transactional
    public void removeFromWishlist(String email, Long productId) {
        User user = findUser(email);
        WishlistItem item = wishlistRepository.findByUserIdAndProductId(user.getId(), productId)
                .orElseThrow(() -> new ResourceNotFoundException("WishlistItem", "productId", productId));
        wishlistRepository.delete(item);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private ProductResponse mapProductToResponse(Product product) {
        List<ProductResponse.ProductImageResponse> images = product.getImages().stream()
                .map(img -> ProductResponse.ProductImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .isPrimary(img.isPrimary())
                        .displayOrder(img.getDisplayOrder())
                        .build())
                .toList();

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .price(product.getPrice())
                .category(product.getCategory())
                .stock(product.getStockQuantity())
                .imageUrls(images)
                .build();
    }
}

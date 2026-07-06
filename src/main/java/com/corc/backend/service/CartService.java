package com.corc.backend.service;

import com.corc.backend.dto.request.CartItemRequest;
import com.corc.backend.dto.response.CartItemResponse;
import com.corc.backend.entity.CartItem;
import com.corc.backend.entity.Product;
import com.corc.backend.entity.User;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.CartItemRepository;
import com.corc.backend.repository.ProductRepository;
import com.corc.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<CartItemResponse> getCart(String email) {
        User user = findUser(email);
        return cartItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional
    public CartItemResponse addToCart(String email, CartItemRequest request) {
        User user = findUser(email);
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", request.getProductId()));

        String uniqueKey = product.getId() + "-" + request.getSize();
        if (request.getCustomDesign() != null) {
            uniqueKey = product.getId() + "-" + request.getSize() + "-custom-" + System.currentTimeMillis();
        }

        Optional<CartItem> existing = cartItemRepository.findByUserIdAndUniqueKey(user.getId(), uniqueKey);
        if (existing.isPresent()) {
            CartItem item = existing.get();
            item.setQuantity(item.getQuantity() + request.getQuantity());
            return mapToResponse(cartItemRepository.save(item));
        }

        CartItem cartItem = CartItem.builder()
                .user(user)
                .product(product)
                .uniqueKey(uniqueKey)
                .size(request.getSize())
                .quantity(request.getQuantity())
                .customDesign(request.getCustomDesign())
                .color(request.getColor())
                .build();

        return mapToResponse(cartItemRepository.save(cartItem));
    }

    @Transactional
    public CartItemResponse updateQuantity(String email, String uniqueKey, int quantity) {
        User user = findUser(email);
        CartItem item = cartItemRepository.findByUserIdAndUniqueKey(user.getId(), uniqueKey)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "uniqueKey", uniqueKey));

        item.setQuantity(quantity);
        return mapToResponse(cartItemRepository.save(item));
    }

    @Transactional
    public void removeFromCart(String email, String uniqueKey) {
        User user = findUser(email);
        CartItem item = cartItemRepository.findByUserIdAndUniqueKey(user.getId(), uniqueKey)
                .orElseThrow(() -> new ResourceNotFoundException("CartItem", "uniqueKey", uniqueKey));
        cartItemRepository.delete(item);
    }

    @Transactional
    public void clearCart(Long userId) {
        cartItemRepository.deleteAllByUserId(userId);
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    private CartItemResponse mapToResponse(CartItem item) {
        Product product = item.getProduct();
        String primaryImage = product.getImages().stream()
                .filter(img -> img.isPrimary())
                .findFirst()
                .map(img -> img.getImageUrl())
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());

        return CartItemResponse.builder()
                .id(item.getId())
                .uniqueKey(item.getUniqueKey())
                .productId(product.getId())
                .name(product.getName())
                .price(product.getPrice())
                .image(primaryImage)
                .category(product.getCategory())
                .size(item.getSize())
                .quantity(item.getQuantity())
                .customDesign(item.getCustomDesign())
                .color(item.getColor())
                .build();
    }
}

package com.corc.backend.service;

import com.corc.backend.dto.request.PlaceOrderRequest;
import com.corc.backend.dto.response.OrderResponse;
import com.corc.backend.entity.*;
import com.corc.backend.entity.enums.OrderStatus;
import com.corc.backend.entity.enums.PaymentStatus;
import com.corc.backend.entity.enums.StockAdjustmentReason;
import com.corc.backend.exception.InsufficientStockException;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CouponRepository couponRepository;
    private final StockAdjustmentLogRepository stockLogRepository;

    private static final DateTimeFormatter DISPLAY_FORMAT =
            DateTimeFormatter.ofPattern("MMM d, h:mm a", Locale.ENGLISH);

    @Transactional
    public OrderResponse placeOrder(String email, PlaceOrderRequest request) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));

        if (request.getIdempotencyKey() != null) {
            var existing = orderRepository.findByIdempotencyKey(request.getIdempotencyKey());
            if (existing.isPresent()) {
                return mapToResponse(existing.get());
            }
        }

        List<CartItem> cartItems = cartItemRepository.findByUserIdOrderByCreatedAtDesc(user.getId());
        if (cartItems.isEmpty()) {
            throw new IllegalArgumentException("Cart is empty");
        }

        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (CartItem cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product.getStockQuantity() < cartItem.getQuantity()) {
                throw new InsufficientStockException(
                        "Insufficient stock for " + product.getName() +
                        ". Available: " + product.getStockQuantity());
            }

            int prevQty = product.getStockQuantity();
            product.setStockQuantity(prevQty - cartItem.getQuantity());
            productRepository.save(product);

            stockLogRepository.save(StockAdjustmentLog.builder()
                    .product(product)
                    .quantityChanged(-cartItem.getQuantity())
                    .previousQuantity(prevQty)
                    .newQuantity(product.getStockQuantity())
                    .reason(StockAdjustmentReason.SALE)
                    .adjustedBy(user)
                    .build());

            BigDecimal lineTotal = product.getPrice()
                    .multiply(BigDecimal.valueOf(cartItem.getQuantity()));

            String primaryImage = product.getImages().stream()
                    .filter(ProductImage::isPrimary)
                    .findFirst()
                    .map(ProductImage::getImageUrl)
                    .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0).getImageUrl());

            OrderItem orderItem = OrderItem.builder()
                    .product(product)
                    .productName(product.getName())
                    .productImage(primaryImage)
                    .unitPrice(product.getPrice())
                    .quantity(cartItem.getQuantity())
                    .size(cartItem.getSize())
                    .customDesign(cartItem.getCustomDesign())
                    .lineTotal(lineTotal)
                    .build();

            orderItems.add(orderItem);
            subtotal = subtotal.add(lineTotal);
        }

        BigDecimal discount = BigDecimal.ZERO;
        String couponCode = null;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            var couponOpt = couponRepository.findByCodeIgnoreCase(request.getCouponCode());
            if (couponOpt.isPresent()) {
                Coupon coupon = couponOpt.get();
                if (coupon.isActive()
                        && (coupon.getMaxUses() == null || coupon.getCurrentUses() < coupon.getMaxUses())
                        && (coupon.getMinimumOrder() == null || subtotal.compareTo(coupon.getMinimumOrder()) >= 0)) {

                    if (coupon.getDiscountPercent() != null) {
                        discount = subtotal.multiply(coupon.getDiscountPercent())
                                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                    } else if (coupon.getDiscountAmount() != null) {
                        discount = coupon.getDiscountAmount();
                    }

                    coupon.setCurrentUses(coupon.getCurrentUses() + 1);
                    couponRepository.save(coupon);
                    couponCode = coupon.getCode();
                }
            }
        }

        BigDecimal total = subtotal.subtract(discount);
        if (total.compareTo(BigDecimal.ZERO) < 0) total = BigDecimal.ZERO;

        String trackingNumber = "ORD-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        Order order = Order.builder()
                .user(user)
                .trackingNumber(trackingNumber)
                .idempotencyKey(request.getIdempotencyKey())
                .status(OrderStatus.ORDERED)
                .subtotal(subtotal)
                .discount(discount)
                .shippingCost(BigDecimal.ZERO)
                .total(total)
                .couponCode(couponCode)
                .shippingAddress(request.getShippingAddress())
                .paymentStatus(PaymentStatus.COMPLETED)
                .orderedAt(Instant.now())
                .build();

        for (OrderItem item : orderItems) {
            order.addItem(item);
        }

        order = orderRepository.save(order);
        cartItemRepository.deleteAllByUserId(user.getId());

        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getUserOrders(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
        return orderRepository.findByUserIdOrderByCreatedAtDesc(user.getId()).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByTracking(String trackingNumber) {
        Order order = orderRepository.findByTrackingNumber(trackingNumber)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "trackingNumber", trackingNumber));
        return mapToResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(Long id) {
        Order order = orderRepository.findByIdWithItems(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order", "id", id));
        return mapToResponse(order);
    }

    private OrderResponse mapToResponse(Order order) {
        List<OrderResponse.OrderItemResponse> items = order.getItems().stream()
                .map(item -> OrderResponse.OrderItemResponse.builder()
                        .id(item.getId())
                        .productId(item.getProduct().getId())
                        .productName(item.getProductName())
                        .productImage(item.getProductImage())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .size(item.getSize())
                        .lineTotal(item.getLineTotal())
                        .build())
                .toList();

        List<OrderResponse.OrderMilestone> milestones = buildMilestones(order);

        return OrderResponse.builder()
                .id(order.getId())
                .trackingNumber(order.getTrackingNumber())
                .status(order.getStatus().name())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingCost(order.getShippingCost())
                .total(order.getTotal())
                .couponCode(order.getCouponCode())
                .shippingAddress(order.getShippingAddress())
                .paymentStatus(order.getPaymentStatus().name())
                .items(items)
                .milestones(milestones)
                .createdAt(order.getCreatedAt() != null ? order.getCreatedAt().toString() : null)
                .build();
    }

    private List<OrderResponse.OrderMilestone> buildMilestones(Order order) {
        List<OrderResponse.OrderMilestone> milestones = new ArrayList<>();
        OrderStatus current = order.getStatus();

        milestones.add(OrderResponse.OrderMilestone.builder()
                .status("ORDERED").label("Order Placed")
                .date(formatDate(order.getOrderedAt()))
                .done(current.ordinal() >= OrderStatus.ORDERED.ordinal())
                .build());

        milestones.add(OrderResponse.OrderMilestone.builder()
                .status("PROCESSING").label("Processing")
                .date(formatDate(order.getProcessedAt()))
                .done(current.ordinal() >= OrderStatus.PROCESSING.ordinal())
                .build());

        milestones.add(OrderResponse.OrderMilestone.builder()
                .status("SHIPPED").label("Shipped")
                .date(formatDate(order.getShippedAt()))
                .done(current.ordinal() >= OrderStatus.SHIPPED.ordinal())
                .build());

        milestones.add(OrderResponse.OrderMilestone.builder()
                .status("DELIVERED").label("Delivered")
                .date(formatDate(order.getDeliveredAt()))
                .done(current.ordinal() >= OrderStatus.DELIVERED.ordinal())
                .build());

        return milestones;
    }

    private String formatDate(Instant instant) {
        if (instant == null) return null;
        return instant.atOffset(ZoneOffset.UTC).format(DISPLAY_FORMAT);
    }
}

package com.corc.backend.service;

import com.corc.backend.dto.request.ProductCreateRequest;
import com.corc.backend.dto.request.ProductUpdateRequest;
import com.corc.backend.dto.response.ProductResponse;
import com.corc.backend.entity.Product;
import com.corc.backend.entity.ProductImage;
import com.corc.backend.exception.ResourceNotFoundException;
import com.corc.backend.repository.ProductRepository;
import com.corc.backend.repository.ProductReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final ProductReviewRepository reviewRepository;

    @Cacheable(value = "products", key = "'all'")
    @Transactional(readOnly = true)
    public List<ProductResponse> getAllProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<ProductResponse> getFilteredProducts(String category, BigDecimal minPrice,
                                                      BigDecimal maxPrice, String search,
                                                      int page, int size, String sortBy) {
        Sort sort = switch (sortBy) {
            case "low" -> Sort.by("price").ascending();
            case "high" -> Sort.by("price").descending();
            default -> Sort.by("createdAt").descending();
        };

        Pageable pageable = PageRequest.of(page, size, sort);
        return productRepository.findByFilters(category, minPrice, maxPrice, search, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public ProductResponse getProductBySlug(String slug) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "slug", slug));
        return mapToResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getRelatedProducts(Long productId, String category) {
        Pageable pageable = PageRequest.of(0, 3);
        return productRepository.findRelatedProducts(category, productId, pageable).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse createProduct(ProductCreateRequest request) {
        String slug = generateSlug(request.getName());

        Product product = Product.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .price(request.getPrice())
                .compareAtPrice(request.getCompareAtPrice())
                .category(request.getCategory())
                .stockQuantity(request.getStock())
                .sku(request.getSku())
                .featured(request.isFeatured())
                .active(true)
                .build();

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            IntStream.range(0, request.getImageUrls().size()).forEach(i -> {
                ProductImage image = ProductImage.builder()
                        .imageUrl(request.getImageUrls().get(i))
                        .displayOrder(i)
                        .isPrimary(i == 0)
                        .build();
                product.addImage(image);
            });
        }

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public ProductResponse updateProduct(Long id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));

        if (request.getName() != null) {
            product.setName(request.getName());
            product.setSlug(generateSlug(request.getName()));
        }
        if (request.getCategory() != null) product.setCategory(request.getCategory());
        if (request.getDescription() != null) product.setDescription(request.getDescription());
        if (request.getPrice() != null) product.setPrice(request.getPrice());
        if (request.getCompareAtPrice() != null) product.setCompareAtPrice(request.getCompareAtPrice());
        if (request.getStock() != null) product.setStockQuantity(request.getStock());
        if (request.getSku() != null) product.setSku(request.getSku());
        if (request.getFeatured() != null) product.setFeatured(request.getFeatured());
        if (request.getActive() != null) product.setActive(request.getActive());

        if (request.getImageUrls() != null) {
            product.getImages().clear();
            IntStream.range(0, request.getImageUrls().size()).forEach(i -> {
                ProductImage image = ProductImage.builder()
                        .imageUrl(request.getImageUrls().get(i))
                        .displayOrder(i)
                        .isPrimary(i == 0)
                        .build();
                product.addImage(image);
            });
        }

        Product saved = productRepository.save(product);
        return mapToResponse(saved);
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void deleteProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", "id", id));
        productRepository.delete(product);
    }

    private ProductResponse mapToResponse(Product product) {
        double avgRating = reviewRepository.findAverageRatingByProductId(product.getId());
        long reviewCount = reviewRepository.countByProductId(product.getId());

        List<ProductResponse.ProductImageResponse> imageResponses = product.getImages().stream()
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
                .description(product.getDescription())
                .price(product.getPrice())
                .compareAtPrice(product.getCompareAtPrice())
                .category(product.getCategory())
                .stock(product.getStockQuantity())
                .sku(product.getSku())
                .featured(product.isFeatured())
                .imageUrls(imageResponses)
                .averageRating(Math.round(avgRating * 10.0) / 10.0)
                .reviewCount(reviewCount)
                .createdAt(product.getCreatedAt() != null ? product.getCreatedAt().toString() : null)
                .build();
    }

    private String generateSlug(String name) {
        String base = name.toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
        if (productRepository.findBySlug(base).isPresent()) {
            return base + "-" + System.currentTimeMillis();
        }
        return base;
    }
}

package com.corc.backend.config;

import com.corc.backend.entity.*;
import com.corc.backend.entity.enums.OrderStatus;
import com.corc.backend.entity.enums.PaymentStatus;
import com.corc.backend.entity.enums.RoleName;
import com.corc.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.beans.factory.annotation.Value;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Profile({"!prod"})
@Component
@Order(1)
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppConfigRepository appConfigRepository;
    private final CouponRepository couponRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductReviewRepository productReviewRepository;
    private final OrderRepository orderRepository;
    private final CartItemRepository cartItemRepository;
    private final WishlistItemRepository wishlistItemRepository;
    private final AddressRepository addressRepository;
    private final PaymentCardRepository paymentCardRepository;
private final NewsletterSubscriptionRepository newsletterSubscriptionRepository;

    @Value("${ADMIN_EMAIL:admin@corc.com}")
    private String adminEmail;

    @Value("${ADMIN_PASSWORD}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(String... args) {
        seedRoles();
        seedAdminUser();
        seedAppConfig();
        seedCoupons();
        seedCategories();
        seedProducts();
        seedDemoUsers();
        log.info("Data seeding complete");
    }

    // ──────────────────────────────────────────
    // ROLES
    // ──────────────────────────────────────────

    private void seedRoles() {
        for (RoleName roleName : RoleName.values()) {
            if (roleRepository.findByName(roleName).isEmpty()) {
                roleRepository.save(Role.builder().name(roleName).build());
                log.info("Seeded role: {}", roleName);
            }
        }
    }

    // ──────────────────────────────────────────
    // ADMIN USER
    // ──────────────────────────────────────────

    private void seedAdminUser() {
        String email = this.adminEmail;
        String password = this.adminPassword;
        if (email == null || email.isEmpty()) {
            email = "admin@corc.com"; // fallback, though @Value has default
        }
        if (password == null || password.isEmpty()) {
            log.warn("Admin password not set via ADMIN_PASSWORD environment variable. Skipping admin user creation.");
            return;
        }
        if (userRepository.findByEmail(email).isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN).orElseThrow();
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();

            User admin = User.builder()
                    .name("CORC Admin")
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .phone("+1-555-000-0000")
                    .avatar("https://ui-avatars.com/api/?name=CORC+Admin&background=c6a87c&color=000")
                    .enabled(true)
                    .roles(Set.of(adminRole, userRole))
                    .build();

            userRepository.save(admin);
            log.info("Seeded admin user: {}", email);
        }
    }

    // ──────────────────────────────────────────
    // APP CONFIG
    // ──────────────────────────────────────────

    private void seedAppConfig() {
        seedConfig("announcement", "Season 01 Available Now");
        seedConfig("shippingNote", "Free Worldwide Shipping over $200");
        seedConfig("announcement_bar_text", "Season 01 Available Now | Free Worldwide Shipping over $200");
    }

    private void seedConfig(String key, String value) {
        if (appConfigRepository.findByConfigKey(key).isEmpty()) {
            appConfigRepository.save(AppConfig.builder()
                    .configKey(key)
                    .configValue(value)
                    .build());
        }
    }

    // ──────────────────────────────────────────
    // COUPONS
    // ──────────────────────────────────────────

    private void seedCoupons() {
        if (couponRepository.findByCodeIgnoreCase("CORC20").isEmpty()) {
            couponRepository.save(Coupon.builder()
                    .code("CORC20")
                    .description("20% off your order")
                    .discountPercent(new BigDecimal("20.00"))
                    .active(true)
                    .build());
            log.info("Seeded coupon: CORC20");
        }

        if (couponRepository.findByCodeIgnoreCase("WELCOME10").isEmpty()) {
            couponRepository.save(Coupon.builder()
                    .code("WELCOME10")
                    .description("10% off for new customers")
                    .discountPercent(new BigDecimal("10.00"))
                    .active(true)
                    .build());
            log.info("Seeded coupon: WELCOME10");
        }

        if (couponRepository.findByCodeIgnoreCase("SEASON50").isEmpty()) {
            couponRepository.save(Coupon.builder()
                    .code("SEASON50")
                    .description("Season 01 launch — 50% off")
                    .discountPercent(new BigDecimal("50.00"))
                    .active(true)
                    .build());
            log.info("Seeded coupon: SEASON50");
        }
    }

    // ──────────────────────────────────────────
    // CATEGORIES
    // ──────────────────────────────────────────

    private void seedCategories() {
        List<String[]> cats = List.of(
                new String[]{"Tees", "tees", "Premium heavyweight tees"},
                new String[]{"Hoodies", "hoodies", "Oversized luxury hoodies"},
                new String[]{"Jackets", "jackets", "Statement outerwear"},
                new String[]{"Pants", "pants", "Tailored streetwear trousers"},
                new String[]{"Accessories", "accessories", "Finishing touches"}
        );

        for (int i = 0; i < cats.size(); i++) {
            String[] c = cats.get(i);
            if (categoryRepository.findBySlug(c[1]).isEmpty()) {
                categoryRepository.save(Category.builder()
                        .name(c[0])
                        .slug(c[1])
                        .description(c[2])
                        .active(true)
                        .displayOrder(i)
                        .build());
                log.info("Seeded category: {}", c[0]);
            }
        }
    }

    // ──────────────────────────────────────────
    // PRODUCTS
    // ──────────────────────────────────────────

    private void seedProducts() {
        if (productRepository.count() > 0) {
            return;
        }

        createProduct("Shadow Oversized Tee", "shadow-oversized-tee", "Tees",
                "Heavyweight 280gsm cotton. Drop shoulder. Box fit. Ribbed neckline with CORC embossed tag.",
                45, 65, 30, true,
                "https://images.unsplash.com/photo-1521572163474-6864f9cf17ab?w=800",
                "https://images.unsplash.com/photo-1583743814966-8936f5b7be1a?w=800");

        createProduct("Eternal Black Hoodie", "eternal-black-hoodie", "Hoodies",
                "400gsm French terry. Kangaroo pocket. Oversized hood with waxed drawcords. Heavy metal eyelets.",
                120, 160, 20, true,
                "https://images.unsplash.com/photo-1556821840-3a63f95609a7?w=800",
                "https://images.unsplash.com/photo-1578768079470-c6c3bac3f4c7?w=800");

        createProduct("CORC Logo Tee — White", "corc-logo-tee-white", "Tees",
                "Screen-printed CORC emblem on premium 240gsm cotton. Relaxed fit.",
                40, null, 50, false,
                "https://images.unsplash.com/photo-1618354691373-d851c5c3a990?w=800",
                "https://images.unsplash.com/photo-1562157873-818bc0726f68?w=800");

        createProduct("Urban Decay Jacket", "urban-decay-jacket", "Jackets",
                "Washed canvas shell. YKK zippers. Articulated sleeves. Lined with quilted satin.",
                195, 250, 12, true,
                "https://images.unsplash.com/photo-1551028719-00167b16eac5?w=800",
                "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?w=800");

        createProduct("Obsidian Cargo Pants", "obsidian-cargo-pants", "Pants",
                "Tapered cargo silhouette. Ripstop nylon blend. Adjustable ankle cuffs. 6 utility pockets.",
                110, null, 25, false,
                "https://images.unsplash.com/photo-1624378439575-d8705ad7ae80?w=800",
                "https://images.unsplash.com/photo-1473966968600-fa801b869a1a?w=800");

        createProduct("Midnight Crew Neck", "midnight-crew-neck", "Tees",
                "Garment-dyed vintage wash. 260gsm ringspun cotton. Dropped hem.",
                55, 75, 35, true,
                "https://images.unsplash.com/photo-1576566588028-4147f3842f27?w=800",
                "https://images.unsplash.com/photo-1529374255404-311a2a4f1fd9?w=800");

        createProduct("Phantom Zip Hoodie", "phantom-zip-hoodie", "Hoodies",
                "Full zip. Brushed fleece interior. Split kangaroo pocket. Metal CORC badge on chest.",
                135, null, 15, false,
                "https://images.unsplash.com/photo-1620799140408-edc6dcb6d633?w=800",
                "https://images.unsplash.com/photo-1542406775-ade58c52d2e4?w=800");

        createProduct("Concrete Joggers", "concrete-joggers", "Pants",
                "Heavyweight 350gsm terry. Elastic waistband with internal drawcord. Ribbed cuffs.",
                85, 110, 40, false,
                "https://images.unsplash.com/photo-1552902865-b72c031ac5ea?w=800",
                "https://images.unsplash.com/photo-1580906853305-0e453a40d55e?w=800");

        createProduct("CORC Chain Necklace", "corc-chain-necklace", "Accessories",
                "316L surgical steel. 5mm Cuban link. 60cm length. Laser-engraved CORC clasp.",
                75, null, 50, true,
                "https://images.unsplash.com/photo-1599643478518-a784e5dc4c8f?w=800",
                "https://images.unsplash.com/photo-1611652022419-a9419f74343d?w=800");

        createProduct("Stealth Bomber Jacket", "stealth-bomber-jacket", "Jackets",
                "MA-1 silhouette. Water-resistant shell. Orange satin lining. Arm utility pocket.",
                175, 220, 8, true,
                "https://images.unsplash.com/photo-1548126032-079a0fb0099d?w=800",
                "https://images.unsplash.com/photo-1520975954732-35dd22299614?w=800");

        createProduct("Raw Hem Distressed Tee", "raw-hem-distressed-tee", "Tees",
                "Acid wash finish. Hand-distressed hems. 220gsm cotton. Relaxed boxy fit.",
                50, null, 45, false,
                "https://images.unsplash.com/photo-1503341504253-dff4f37263d8?w=800",
                "https://images.unsplash.com/photo-1622445275463-afa2ab738c34?w=800");

        createProduct("Onyx Beanie", "onyx-beanie", "Accessories",
                "Merino wool blend. Ribbed knit. Leather CORC patch. One size fits all.",
                35, null, 60, false,
                "https://images.unsplash.com/photo-1576871337632-b9aef4c17ab9?w=800",
                "https://images.unsplash.com/photo-1588850561407-ed78c334e67a?w=800");

        log.info("Seeded 12 products");
    }

    private Product createProduct(String name, String slug, String category,
                               String description, int price, Integer compareAt,
                               int stock, boolean featured,
                               String... imageUrls) {
        Product product = Product.builder()
                .name(name)
                .slug(slug)
                .category(category)
                .description(description)
                .price(new BigDecimal(price))
                .compareAtPrice(compareAt != null ? new BigDecimal(compareAt) : null)
                .stockQuantity(stock)
                .lowStockThreshold(5)
                .sku("CORC-" + slug.toUpperCase().replace("-", "").substring(0, Math.min(8, slug.length())))
                .featured(featured)
                .active(true)
                .build();

        product = productRepository.save(product);

        for (int i = 0; i < imageUrls.length; i++) {
            ProductImage img = ProductImage.builder()
                    .product(product)
                    .imageUrl(imageUrls[i])
                    .displayOrder(i)
                    .isPrimary(i == 0)
                    .build();
            productImageRepository.save(img);
        }

        return product;
    }

    // ──────────────────────────────────────────
    // DEMO USERS + ALL RELATED DATA
    // ──────────────────────────────────────────

    private void seedDemoUsers() {
        if (userRepository.findByEmail("john@example.com").isPresent()) {
            return;
        }

        Role userRole = roleRepository.findByName(RoleName.ROLE_USER).orElseThrow();
        List<Product> products = productRepository.findAll();
        Instant now = Instant.now();

        // ── User 1: John (active buyer with order history) ──
        User john = userRepository.save(User.builder()
                .name("John Mitchell")
                .email("john@example.com")
                .password(passwordEncoder.encode("password123"))
                .phone("+1-555-123-4567")
                .avatar("https://ui-avatars.com/api/?name=John+Mitchell&background=4a90d9&color=fff")
                .enabled(true)
                .roles(Set.of(userRole))
                .build());

        // ── User 2: Sarah (new customer, browsing) ──
        User sarah = userRepository.save(User.builder()
                .name("Sarah Chen")
                .email("sarah@example.com")
                .password(passwordEncoder.encode("password123"))
                .phone("+44-20-7946-0958")
                .avatar("https://ui-avatars.com/api/?name=Sarah+Chen&background=e91e63&color=fff")
                .enabled(true)
                .roles(Set.of(userRole))
                .build());

        // ── User 3: Marcus (returning customer) ──
        User marcus = userRepository.save(User.builder()
                .name("Marcus Rivera")
                .email("marcus@example.com")
                .password(passwordEncoder.encode("password123"))
                .phone("+1-555-987-6543")
                .avatar("https://ui-avatars.com/api/?name=Marcus+Rivera&background=ff9800&color=000")
                .enabled(true)
                .roles(Set.of(userRole))
                .build());

        log.info("Seeded 3 demo users");

        // ── ADDRESSES ──
        addressRepository.save(Address.builder()
                .user(john).type("Home")
                .street("742 Evergreen Terrace").city("New York").zip("10001").country("United States")
                .isDefault(true).build());
        addressRepository.save(Address.builder()
                .user(john).type("Work")
                .street("350 Fifth Avenue, Suite 3100").city("New York").zip("10118").country("United States")
                .isDefault(false).build());
        addressRepository.save(Address.builder()
                .user(sarah).type("Home")
                .street("221B Baker Street").city("London").zip("NW1 6XE").country("United Kingdom")
                .isDefault(true).build());
        addressRepository.save(Address.builder()
                .user(marcus).type("Home")
                .street("1600 Pennsylvania Avenue").city("Los Angeles").zip("90001").country("United States")
                .isDefault(true).build());
        addressRepository.save(Address.builder()
                .user(marcus).type("Office")
                .street("888 Brannan Street").city("San Francisco").zip("94103").country("United States")
                .isDefault(false).build());
        log.info("Seeded 5 addresses");

        // ── PAYMENT CARDS ──
        paymentCardRepository.save(PaymentCard.builder()
                .user(john).type("Visa").last4("4242").expiry("12/27").isDefault(true).build());
        paymentCardRepository.save(PaymentCard.builder()
                .user(john).type("Mastercard").last4("8888").expiry("06/28").isDefault(false).build());
        paymentCardRepository.save(PaymentCard.builder()
                .user(sarah).type("Visa").last4("1234").expiry("03/29").isDefault(true).build());
        paymentCardRepository.save(PaymentCard.builder()
                .user(marcus).type("Amex").last4("3782").expiry("09/27").isDefault(true).build());
        log.info("Seeded 4 payment cards");

        // ── PRODUCT REVIEWS ──
        Product shadowTee = products.stream().filter(p -> p.getSlug().equals("shadow-oversized-tee")).findFirst().orElse(null);
        Product eternalHoodie = products.stream().filter(p -> p.getSlug().equals("eternal-black-hoodie")).findFirst().orElse(null);
        Product urbanJacket = products.stream().filter(p -> p.getSlug().equals("urban-decay-jacket")).findFirst().orElse(null);
        Product midnightCrew = products.stream().filter(p -> p.getSlug().equals("midnight-crew-neck")).findFirst().orElse(null);
        Product cargoPants = products.stream().filter(p -> p.getSlug().equals("obsidian-cargo-pants")).findFirst().orElse(null);
        Product chainNecklace = products.stream().filter(p -> p.getSlug().equals("corc-chain-necklace")).findFirst().orElse(null);
        Product bomberJacket = products.stream().filter(p -> p.getSlug().equals("stealth-bomber-jacket")).findFirst().orElse(null);
        Product phantomHoodie = products.stream().filter(p -> p.getSlug().equals("phantom-zip-hoodie")).findFirst().orElse(null);
        Product joggers = products.stream().filter(p -> p.getSlug().equals("concrete-joggers")).findFirst().orElse(null);
        Product beanie = products.stream().filter(p -> p.getSlug().equals("onyx-beanie")).findFirst().orElse(null);

        if (shadowTee != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(shadowTee).user(john).rating(5)
                    .text("Insane quality for the price. The 280gsm fabric feels like twice the cost. Fits oversized as expected — went TTS and love the drape.")
                    .build());
            productReviewRepository.save(ProductReview.builder()
                    .product(shadowTee).user(sarah).rating(4)
                    .text("Beautiful tee, very thick cotton. Only reason for 4 stars is it runs a bit large. Size down if you want a less boxy look.")
                    .build());
            productReviewRepository.save(ProductReview.builder()
                    .product(shadowTee).user(marcus).rating(5)
                    .text("Third CORC tee I've bought. The ribbed neckline detail is chef's kiss. Already ordered another in grey.")
                    .build());
        }

        if (eternalHoodie != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(eternalHoodie).user(john).rating(5)
                    .text("This hoodie is HEAVY. In the best way possible. The waxed drawcords and metal eyelets give it an industrial feel I haven't seen anywhere else.")
                    .build());
            productReviewRepository.save(ProductReview.builder()
                    .product(eternalHoodie).user(marcus).rating(4)
                    .text("Premium quality, worth every penny. The French terry interior is incredibly soft. Just wish they had more color options.")
                    .build());
        }

        if (urbanJacket != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(urbanJacket).user(sarah).rating(5)
                    .text("Statement piece. The quilted satin lining was a surprise — makes it feel truly luxury. YKK zippers are buttery smooth.")
                    .build());
            productReviewRepository.save(ProductReview.builder()
                    .product(urbanJacket).user(john).rating(5)
                    .text("Worth the splurge. This jacket gets compliments every single time I wear it. The washed canvas breaks in beautifully.")
                    .build());
        }

        if (midnightCrew != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(midnightCrew).user(marcus).rating(5)
                    .text("The garment-dye gives it such a unique look. No two are exactly alike. Love the dropped hem — stays tucked or looks good untucked.")
                    .build());
        }

        if (cargoPants != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(cargoPants).user(john).rating(4)
                    .text("Great tapered fit, doesn't look like typical baggy cargos. The ripstop is durable but breathable. Adjustable cuffs are a nice touch.")
                    .build());
            productReviewRepository.save(ProductReview.builder()
                    .product(cargoPants).user(sarah).rating(5)
                    .text("Finally cargos that look good on women too. The utility pockets actually hold things without looking bulky. Sizing is accurate.")
                    .build());
        }

        if (chainNecklace != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(chainNecklace).user(marcus).rating(5)
                    .text("Surgical steel means no green neck. The Cuban link weight is perfect — substantial but not obnoxious. Laser-engraved clasp is a nice detail.")
                    .build());
        }

        if (bomberJacket != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(bomberJacket).user(john).rating(5)
                    .text("The orange satin lining peeking out when unzipped is fire. Water-resistant shell saved me in a rainstorm. Arm pocket fits my phone perfectly.")
                    .build());
            productReviewRepository.save(ProductReview.builder()
                    .product(bomberJacket).user(sarah).rating(4)
                    .text("Love the MA-1 silhouette updated with modern details. Slightly bulky on smaller frames but the quality is undeniable.")
                    .build());
        }

        if (phantomHoodie != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(phantomHoodie).user(sarah).rating(5)
                    .text("The brushed fleece interior is like wearing a blanket. Metal CORC badge adds just enough branding without being loud.")
                    .build());
        }

        if (joggers != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(joggers).user(marcus).rating(4)
                    .text("Comfortable enough for lounging, stylish enough for errands. The 350gsm weight keeps shape after multiple washes.")
                    .build());
        }

        if (beanie != null) {
            productReviewRepository.save(ProductReview.builder()
                    .product(beanie).user(john).rating(5)
                    .text("Merino wool keeps warm without being itchy. The leather patch is subtle — exactly how I like my branding. One size fits my big head fine.")
                    .build());
        }

        log.info("Seeded 17 product reviews");

        // ── ORDERS ──

        // John's delivered order (placed 12 days ago)
        if (shadowTee != null && eternalHoodie != null) {
            String primaryImg1 = getFirstImageUrl(shadowTee);
            String primaryImg2 = getFirstImageUrl(eternalHoodie);

            com.corc.backend.entity.Order order1 = com.corc.backend.entity.Order.builder()
                    .trackingNumber("CORC-2026-00001")
                    .idempotencyKey("seed-order-001")
                    .user(john)
                    .status(OrderStatus.DELIVERED)
                    .subtotal(new BigDecimal("165.00"))
                    .discount(new BigDecimal("33.00"))
                    .shippingCost(BigDecimal.ZERO)
                    .total(new BigDecimal("132.00"))
                    .couponCode("CORC20")
                    .shippingAddress("742 Evergreen Terrace, New York, NY 10001, United States")
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .orderedAt(now.minus(12, ChronoUnit.DAYS))
                    .processedAt(now.minus(11, ChronoUnit.DAYS))
                    .shippedAt(now.minus(8, ChronoUnit.DAYS))
                    .deliveredAt(now.minus(3, ChronoUnit.DAYS))
                    .build();

            order1.addItem(OrderItem.builder()
                    .product(shadowTee).productName(shadowTee.getName()).productImage(primaryImg1)
                    .unitPrice(shadowTee.getPrice()).quantity(1).size("L")
                    .lineTotal(shadowTee.getPrice()).build());
            order1.addItem(OrderItem.builder()
                    .product(eternalHoodie).productName(eternalHoodie.getName()).productImage(primaryImg2)
                    .unitPrice(eternalHoodie.getPrice()).quantity(1).size("XL")
                    .lineTotal(eternalHoodie.getPrice()).build());

            orderRepository.save(order1);
        }

        // John's shipped order (placed 3 days ago)
        if (urbanJacket != null && chainNecklace != null) {
            String primaryImg1 = getFirstImageUrl(urbanJacket);
            String primaryImg2 = getFirstImageUrl(chainNecklace);

            com.corc.backend.entity.Order order2 = com.corc.backend.entity.Order.builder()
                    .trackingNumber("CORC-2026-00002")
                    .idempotencyKey("seed-order-002")
                    .user(john)
                    .status(OrderStatus.SHIPPED)
                    .subtotal(new BigDecimal("270.00"))
                    .discount(BigDecimal.ZERO)
                    .shippingCost(BigDecimal.ZERO)
                    .total(new BigDecimal("270.00"))
                    .shippingAddress("350 Fifth Avenue, Suite 3100, New York, NY 10118, United States")
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .orderedAt(now.minus(3, ChronoUnit.DAYS))
                    .processedAt(now.minus(2, ChronoUnit.DAYS))
                    .shippedAt(now.minus(1, ChronoUnit.DAYS))
                    .build();

            order2.addItem(OrderItem.builder()
                    .product(urbanJacket).productName(urbanJacket.getName()).productImage(primaryImg1)
                    .unitPrice(urbanJacket.getPrice()).quantity(1).size("M")
                    .lineTotal(urbanJacket.getPrice()).build());
            order2.addItem(OrderItem.builder()
                    .product(chainNecklace).productName(chainNecklace.getName()).productImage(primaryImg2)
                    .unitPrice(chainNecklace.getPrice()).quantity(1).size("OS")
                    .lineTotal(chainNecklace.getPrice()).build());

            orderRepository.save(order2);
        }

        // Sarah's processing order (placed today)
        if (bomberJacket != null && midnightCrew != null && beanie != null) {
            com.corc.backend.entity.Order order3 = com.corc.backend.entity.Order.builder()
                    .trackingNumber("CORC-2026-00003")
                    .idempotencyKey("seed-order-003")
                    .user(sarah)
                    .status(OrderStatus.PROCESSING)
                    .subtotal(new BigDecimal("265.00"))
                    .discount(new BigDecimal("26.50"))
                    .shippingCost(BigDecimal.ZERO)
                    .total(new BigDecimal("238.50"))
                    .couponCode("WELCOME10")
                    .shippingAddress("221B Baker Street, London, NW1 6XE, United Kingdom")
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .orderedAt(now.minus(6, ChronoUnit.HOURS))
                    .processedAt(now.minus(2, ChronoUnit.HOURS))
                    .build();

            order3.addItem(OrderItem.builder()
                    .product(bomberJacket).productName(bomberJacket.getName()).productImage(getFirstImageUrl(bomberJacket))
                    .unitPrice(bomberJacket.getPrice()).quantity(1).size("S")
                    .lineTotal(bomberJacket.getPrice()).build());
            order3.addItem(OrderItem.builder()
                    .product(midnightCrew).productName(midnightCrew.getName()).productImage(getFirstImageUrl(midnightCrew))
                    .unitPrice(midnightCrew.getPrice()).quantity(1).size("M")
                    .lineTotal(midnightCrew.getPrice()).build());
            order3.addItem(OrderItem.builder()
                    .product(beanie).productName(beanie.getName()).productImage(getFirstImageUrl(beanie))
                    .unitPrice(beanie.getPrice()).quantity(1).size("OS")
                    .lineTotal(beanie.getPrice()).build());

            orderRepository.save(order3);
        }

        // Marcus's delivered order (placed 20 days ago)
        if (cargoPants != null && joggers != null) {
            com.corc.backend.entity.Order order4 = com.corc.backend.entity.Order.builder()
                    .trackingNumber("CORC-2026-00004")
                    .idempotencyKey("seed-order-004")
                    .user(marcus)
                    .status(OrderStatus.DELIVERED)
                    .subtotal(new BigDecimal("195.00"))
                    .discount(BigDecimal.ZERO)
                    .shippingCost(BigDecimal.ZERO)
                    .total(new BigDecimal("195.00"))
                    .shippingAddress("1600 Pennsylvania Avenue, Los Angeles, CA 90001, United States")
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .orderedAt(now.minus(20, ChronoUnit.DAYS))
                    .processedAt(now.minus(19, ChronoUnit.DAYS))
                    .shippedAt(now.minus(16, ChronoUnit.DAYS))
                    .deliveredAt(now.minus(12, ChronoUnit.DAYS))
                    .build();

            order4.addItem(OrderItem.builder()
                    .product(cargoPants).productName(cargoPants.getName()).productImage(getFirstImageUrl(cargoPants))
                    .unitPrice(cargoPants.getPrice()).quantity(1).size("L")
                    .lineTotal(cargoPants.getPrice()).build());
            order4.addItem(OrderItem.builder()
                    .product(joggers).productName(joggers.getName()).productImage(getFirstImageUrl(joggers))
                    .unitPrice(joggers.getPrice()).quantity(1).size("L")
                    .lineTotal(joggers.getPrice()).build());

            orderRepository.save(order4);
        }

        // Marcus's new order (just placed)
        if (eternalHoodie != null && phantomHoodie != null) {
            com.corc.backend.entity.Order order5 = com.corc.backend.entity.Order.builder()
                    .trackingNumber("CORC-2026-00005")
                    .idempotencyKey("seed-order-005")
                    .user(marcus)
                    .status(OrderStatus.ORDERED)
                    .subtotal(new BigDecimal("255.00"))
                    .discount(new BigDecimal("127.50"))
                    .shippingCost(BigDecimal.ZERO)
                    .total(new BigDecimal("127.50"))
                    .couponCode("SEASON50")
                    .shippingAddress("888 Brannan Street, San Francisco, CA 94103, United States")
                    .paymentStatus(PaymentStatus.COMPLETED)
                    .orderedAt(now.minus(1, ChronoUnit.HOURS))
                    .build();

            order5.addItem(OrderItem.builder()
                    .product(eternalHoodie).productName(eternalHoodie.getName()).productImage(getFirstImageUrl(eternalHoodie))
                    .unitPrice(eternalHoodie.getPrice()).quantity(1).size("L")
                    .lineTotal(eternalHoodie.getPrice()).build());
            order5.addItem(OrderItem.builder()
                    .product(phantomHoodie).productName(phantomHoodie.getName()).productImage(getFirstImageUrl(phantomHoodie))
                    .unitPrice(phantomHoodie.getPrice()).quantity(1).size("L")
                    .lineTotal(phantomHoodie.getPrice()).build());

            orderRepository.save(order5);
        }

        log.info("Seeded 5 orders");

        // ── CART ITEMS (Sarah has items in cart — she's browsing) ──
        if (shadowTee != null) {
            cartItemRepository.save(CartItem.builder()
                    .user(sarah).product(shadowTee)
                    .uniqueKey(shadowTee.getId() + "-M")
                    .size("M").quantity(1).build());
        }
        if (cargoPants != null) {
            cartItemRepository.save(CartItem.builder()
                    .user(sarah).product(cargoPants)
                    .uniqueKey(cargoPants.getId() + "-S")
                    .size("S").quantity(1).build());
        }
        if (chainNecklace != null) {
            cartItemRepository.save(CartItem.builder()
                    .user(sarah).product(chainNecklace)
                    .uniqueKey(chainNecklace.getId() + "-OS")
                    .size("OS").quantity(2).build());
        }
        log.info("Seeded 3 cart items for Sarah");

        // ── WISHLIST ITEMS ──
        if (urbanJacket != null) {
            wishlistItemRepository.save(WishlistItem.builder().user(sarah).product(urbanJacket).build());
        }
        if (phantomHoodie != null) {
            wishlistItemRepository.save(WishlistItem.builder().user(sarah).product(phantomHoodie).build());
        }
        if (bomberJacket != null) {
            wishlistItemRepository.save(WishlistItem.builder().user(john).product(bomberJacket).build());
        }
        if (joggers != null) {
            wishlistItemRepository.save(WishlistItem.builder().user(john).product(joggers).build());
        }
        if (shadowTee != null) {
            wishlistItemRepository.save(WishlistItem.builder().user(marcus).product(shadowTee).build());
        }
        log.info("Seeded 5 wishlist items");

        // ── NEWSLETTER SUBSCRIPTIONS ──
        if (!newsletterSubscriptionRepository.existsByEmail("john@example.com")) {
            newsletterSubscriptionRepository.save(NewsletterSubscription.builder()
                    .email("john@example.com").active(true).build());
        }
        if (!newsletterSubscriptionRepository.existsByEmail("sarah@example.com")) {
            newsletterSubscriptionRepository.save(NewsletterSubscription.builder()
                    .email("sarah@example.com").active(true).build());
        }
        if (!newsletterSubscriptionRepository.existsByEmail("marcus@example.com")) {
            newsletterSubscriptionRepository.save(NewsletterSubscription.builder()
                    .email("marcus@example.com").active(true).build());
        }
        if (!newsletterSubscriptionRepository.existsByEmail("fashion.lover@gmail.com")) {
            newsletterSubscriptionRepository.save(NewsletterSubscription.builder()
                    .email("fashion.lover@gmail.com").active(true).build());
        }
        if (!newsletterSubscriptionRepository.existsByEmail("streetwear.daily@outlook.com")) {
            newsletterSubscriptionRepository.save(NewsletterSubscription.builder()
                    .email("streetwear.daily@outlook.com").active(true).build());
        }
        log.info("Seeded 5 newsletter subscriptions");
    }

    private String getFirstImageUrl(Product product) {
        List<ProductImage> images = productImageRepository.findByProductIdOrderByDisplayOrderAsc(product.getId());
        return images.isEmpty() ? null : images.get(0).getImageUrl();
    }
}

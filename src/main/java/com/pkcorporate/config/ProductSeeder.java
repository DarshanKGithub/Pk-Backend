package com.pkcorporate.config;

import com.pkcorporate.entity.ProductImage;
import com.pkcorporate.entity.TShirtProduct;
import com.pkcorporate.repository.TShirtProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Seeds the default T-shirt product catalog on first boot.
 *
 * Why @Transactional is on run() and NOT on a private helper:
 * ─────────────────────────────────────────────────────────────
 * Spring AOP creates a proxy around the bean. Proxy interception only works on
 * public (or protected, depending on proxy type) methods. @Transactional on a
 * private method is silently IGNORED — Spring never wraps it in a transaction,
 * so any lazy collection access after findByProductCode() will trigger a
 * LazyInitializationException because the JPA session is closed.
 *
 * The cleanest enterprise solution used here:
 *   • Replace p.getImages().isEmpty() with productRepository.hasImagesByProductCode(code)
 *     → this fires a COUNT query directly in the DB; zero lazy-loading in Java.
 *   • Keep @Transactional on the public run() method so every findByProductCode()
 *     + save() pair shares the same session, as a secondary safety net.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProductSeeder implements CommandLineRunner {

    private final TShirtProductRepository productRepository;
    private final Environment environment;

    @Override
    @Transactional          // ← PUBLIC method: Spring AOP CAN proxy this
    public void run(String... args) {
        boolean seedEnabled = Boolean.parseBoolean(environment.getProperty("SEED_ENABLED", "true"));
        if (!seedEnabled) {
            log.info("ProductSeeder skipped (SEED_ENABLED=false)");
            return;
        }

        log.info("Checking and seeding default T-shirt products and images...");

        seedProductWithImages("TSH-001", "Classic Round Neck Tee",
            "Premium 180 GSM combed cotton, pre-shrunk, double-needle stitching for everyday corporate use.",
            "Round Neck", "Cotton", "180 GSM", "Round Neck", "Half Sleeve", 10, new BigDecimal("150.00"), "Generic",
            Arrays.asList("#FFFFFF", "#000000", "#1a1a2e", "#E10600", "#2563eb", "#16a34a", "#d97706"),
            Arrays.asList("XS", "S", "M", "L", "XL", "2XL", "3XL"),
            Arrays.asList("Screen Print", "DTF", "Embroidery", "Sublimation"),
            List.of(
                "/src/assets/tsh11.png",
                "/src/assets/ts12.png",
                "/src/assets/tsh13.png",
                "/src/assets/tsh14 - Copy.png",
                "/src/assets/tsh15.jpg"
            )
        );

        seedProductWithImages("TSH-002", "Classic Polo Tee",
            "Corporate-grade pique fabric with 3-button placket and ribbed collar. Perfect for uniforms.",
            "Polo", "Pique", "220 GSM", "Collar", "Half Sleeve", 10, new BigDecimal("200.00"), "Generic",
            Arrays.asList("#FFFFFF", "#000000", "#1e3a5f", "#E10600", "#065f46"),
            Arrays.asList("S", "M", "L", "XL", "2XL", "3XL"),
            Arrays.asList("Embroidery", "Screen Print", "Heat Transfer"),
            List.of(
                "/src/assets/tsh21.jpeg",
                "/src/assets/tsh22.jpeg",
                "/src/assets/tsh23.jpg",
                "/src/assets/tsh24.jpeg",
                "/src/assets/tsh25.jpg"
            )
        );

        seedProductWithImages("TSH-003", "Classic Hoodie",
            "320 GSM fleece inner, kangaroo pocket, adjustable drawstring hood. Ideal for winter merch.",
            "Hoodie", "Fleece", "320 GSM", "Hood", "Full Sleeve", 10, new BigDecimal("250.00"), "Generic",
            Arrays.asList("#1a1a2e", "#000000", "#374151", "#E10600", "#7c3aed"),
            Arrays.asList("S", "M", "L", "XL", "2XL", "3XL"),
            Arrays.asList("Screen Print", "DTF", "Embroidery"),
            List.of(
                "/src/assets/tsh31.jpeg",
                "/src/assets/tsh32.jpeg",
                "/src/assets/tsh33.jpeg",
                "/src/assets/tsh34.jpeg",
                "/src/assets/tsh35.jpeg"
            )
        );

        seedProductWithImages("TSH-004", "V-Neck Premium Tee",
            "Lightweight 160 GSM combed cotton, perfect for summer corporate events and college wear.",
            "V-Neck", "Cotton", "160 GSM", "V-Neck", "Half Sleeve", 10, new BigDecimal("350.00"), "Generic",
            Arrays.asList("#FFFFFF", "#000000", "#E10600", "#6366f1", "#ec4899"),
            Arrays.asList("XS", "S", "M", "L", "XL", "2XL"),
            Arrays.asList("DTF", "Sublimation", "Screen Print"),
            List.of(
                "/src/assets/tsh41.jpeg",
                "/src/assets/tsh42.jpeg",
                "/src/assets/tsh43.jpeg",
                "/src/assets/tsh44.jpeg",
                "/src/assets/tsh45.jpeg"
            )
        );

        seedProductWithImages("TSH-005", "Heavyweight Cotton Tee",
            "60/40 cotton-poly blend for durability and comfort. Ideal for uniforms and year-round use.",
            "Round Neck", "Cotton-Poly Blend", "200 GSM", "Round Neck", "Half Sleeve", 10, new BigDecimal("450.00"), "Generic",
            Arrays.asList("#FFFFFF", "#000000", "#1e3a5f", "#374151", "#E10600"),
            Arrays.asList("S", "M", "L", "XL", "2XL", "3XL"),
            Arrays.asList("Screen Print", "Embroidery", "DTF"),
            List.of(
                "/src/assets/tsh51.jpeg",
                "/src/assets/tsh52.jpeg",
                "/src/assets/tsh53.jpeg",
                "/src/assets/tsh54.jpeg",
                "/src/assets/tsh55.jpeg"
            )
        );

        seedProductWithImages("TSH-061", "Dri-Fit Polo",
            "Moisture-wicking polyester for sports teams, marathons, and active corporate events.",
            "Polo", "Polyester", "140 GSM", "Round Neck", "Half Sleeve", 10, new BigDecimal("499.00"), "Generic",
            Arrays.asList("#FFFFFF", "#000000", "#E10600", "#0ea5e9", "#16a34a", "#f59e0b"),
            Arrays.asList("XS", "S", "M", "L", "XL", "2XL"),
            Arrays.asList("Sublimation", "DTF", "Screen Print"),
            List.of(
                "/src/assets/tsh611.jpeg",
                "/src/assets/tsh612.jpeg",
                "/src/assets/tsh613.jpeg",
                "/src/assets/tsh614.jpeg",
                "/src/assets/tsh615.jpeg"
            )
        );

        log.info("Product seeding checks completed.");
    }

    // ─────────────────────────────────────────────────────────────────────────
    // PRIVATE helper — intentionally NOT @Transactional (Spring AOP ignores it
    // on private methods). Instead:
    //   1. The enclosing run() is @Transactional, so its session spans all calls.
    //   2. We NEVER call p.getImages() in Java — we ask the DB directly via
    //      productRepository.countImagesByProductCode(code) which fires a COUNT SQL.
    // ─────────────────────────────────────────────────────────────────────────
    private void seedProductWithImages(
            String code, String name, String desc, String category, String fabricType, String gsm,
            String neckType, String sleeveType, int moq, BigDecimal basePrice, String brand,
            List<String> colors, List<String> sizes, List<String> prints, List<String> imageUrls) {

        // ── Step 1: Check image existence via a pure SQL COUNT — no lazy collection touched ──
        if (productRepository.countImagesByProductCode(code) > 0) {
            log.debug("Product {} already has images. Skipping.", code);
            return;
        }

        // ── Step 2: Load or create the product ───────────────────────────────────────────────
        TShirtProduct p = productRepository.findByProductCode(code).orElseGet(() -> {
            log.info("Product {} does not exist. Seeding product and default images...", code);
            return TShirtProduct.builder()
                    .productCode(code)
                    .name(name)
                    .description(desc)
                    .category(category)
                    .fabricType(fabricType)
                    .gsm(gsm)
                    .neckType(neckType)
                    .sleeveType(sleeveType)
                    .minimumOrderQuantity(moq)
                    .basePrice(basePrice)
                    .discountPrice(null)
                    .stockQuantity(0)
                    .brand(brand)
                    .availableColors(colors)
                    .availableSizes(sizes)
                    .printTypes(prints)
                    .active(true)
                    .build();
        });

        log.info("Product {} exists or was created. Adding default images...", code);

        // ── Step 3: Build and attach images ──────────────────────────────────────────────────
        for (int i = 0; i < imageUrls.size(); i++) {
            ProductImage image = ProductImage.builder()
                    .imageUrl(imageUrls.get(i))
                    .isPrimary(i == 0)
                    .sortOrder(i)
                    .build();
            p.addImage(image);
        }
        productRepository.save(p);
    }
}

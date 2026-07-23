package com.pkcorporate.service;

import com.pkcorporate.dto.request.AdminProductCreateOrUpdateRequest;

import com.pkcorporate.dto.response.ProductImageResponse;
import com.pkcorporate.dto.response.ProductResponse;
import com.pkcorporate.entity.ProductImage;
import com.pkcorporate.entity.TShirtProduct;
import com.pkcorporate.exception.BusinessException;
import com.pkcorporate.repository.TShirtProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final TShirtProductRepository productRepository;
    private final CloudinaryService cloudinaryService;

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products",       allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public ProductResponse createProduct(AdminProductCreateOrUpdateRequest req, List<MultipartFile> images, Integer primaryImageIndex) {
        log.info("[ProductService] Creating product: code={}, name={}, basePrice={}, discountPrice={}", 
                req.productCode(), req.name(), req.basePrice(), req.discountPrice());
        
        if (req.discountPrice() != null && req.discountPrice().compareTo(req.basePrice()) > 0) {
            log.error("[ProductService] Validation error: discountPrice {} > basePrice {}", req.discountPrice(), req.basePrice());
            throw new BusinessException("discountPrice cannot be greater than basePrice");
        }

        TShirtProduct product = TShirtProduct.builder()
                .productCode(req.productCode())
                .name(req.name())
                .description(req.description())
                .category(req.category())
                .fabricType(req.fabricType())
                .gsm(req.gsm())
                .neckType(req.neckType())
                .sleeveType(req.sleeveType())
                .minimumOrderQuantity(req.minimumOrderQuantity() != null ? req.minimumOrderQuantity() : 10)
                .basePrice(req.basePrice())
                .discountPrice(req.discountPrice())
                .stockQuantity(req.stockQuantity())
                .brand(req.brand())
                .availableSizes(new ArrayList<>(req.availableSizes()))
                .availableColors(new ArrayList<>(req.availableColors()))
                .printTypes(new ArrayList<>(req.printTypes()))
                .active(req.active() != null ? req.active() : true)
                .build();

        // Persist first to get FK for images
        product = productRepository.save(product);
        log.info("[ProductService] Saved product entity initially to database. Generated ID: {}", product.getId());

        if (images != null && !images.isEmpty()) {
            log.info("[ProductService] Processing {} uploaded images for product ID: {}", images.size(), product.getId());
            List<ProductImage> saved = saveImages(product, images, primaryImageIndex);
            product.getImages().clear();
            saved.forEach(product::addImage);
            product = productRepository.save(product);
            log.info("[ProductService] Saved product entity with {} images. ID: {}", saved.size(), product.getId());
        }

        return toResponse(product);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products",       allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public ProductResponse updateProduct(UUID id, AdminProductCreateOrUpdateRequest req, List<MultipartFile> images, Integer primaryImageIndex) {
        log.info("[ProductService] Updating product {}: code={}, name={}, basePrice={}, discountPrice={}", 
                id, req.productCode(), req.name(), req.basePrice(), req.discountPrice());
        TShirtProduct product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Product not found"));

        if (req.discountPrice() != null && req.discountPrice().compareTo(req.basePrice()) > 0) {
            log.error("[ProductService] Validation error: discountPrice {} > basePrice {}", req.discountPrice(), req.basePrice());
            throw new BusinessException("discountPrice cannot be greater than basePrice");
        }

        // Update fields
        product.setProductCode(req.productCode());
        product.setName(req.name());
        product.setDescription(req.description());
        product.setCategory(req.category());
        product.setFabricType(req.fabricType());
        product.setGsm(req.gsm());
        product.setNeckType(req.neckType());
        product.setSleeveType(req.sleeveType());
        product.setMinimumOrderQuantity(req.minimumOrderQuantity() != null ? req.minimumOrderQuantity() : product.getMinimumOrderQuantity());
        product.setBasePrice(req.basePrice());
        product.setDiscountPrice(req.discountPrice());
        product.setStockQuantity(req.stockQuantity());
        product.setBrand(req.brand());

        product.setAvailableSizes(new ArrayList<>(req.availableSizes()));
        product.setAvailableColors(new ArrayList<>(req.availableColors()));
        product.setPrintTypes(new ArrayList<>(req.printTypes()));
        if (req.active() != null) product.setActive(req.active());

        // Image handling: if images provided, replace all
        if (images != null && !images.isEmpty()) {
            log.info("[ProductService] Updating images for product {}. Replacing existing {} images.", id, product.getImages() != null ? product.getImages().size() : 0);
            // delete old cloudinary images if publicId exists
            if (product.getImages() != null) {
                for (ProductImage img : product.getImages()) {
                    if (img.getCloudinaryPublicId() != null) {
                        log.info("[ProductService] Deleting old image from Cloudinary. Public ID: {}", img.getCloudinaryPublicId());
                        cloudinaryService.deleteFile(img.getCloudinaryPublicId());
                    }
                }
            }

            product.getImages().clear();
            List<ProductImage> saved = saveImages(product, images, primaryImageIndex);
            saved.forEach(product::addImage);
            log.info("[ProductService] Saved {} new images for product {}", saved.size(), id);
        }

        product = productRepository.save(product);
        log.info("[ProductService] Updated product entity persisted to database. ID: {}", id);
        return toResponse(product);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products",       allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public void deleteProduct(UUID id) {
        TShirtProduct product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Product not found"));

        // delete images
        if (product.getImages() != null) {
            for (ProductImage img : product.getImages()) {
                if (img.getCloudinaryPublicId() != null) {
                    cloudinaryService.deleteFile(img.getCloudinaryPublicId());
                }
            }
        }

        productRepository.delete(product);
    }

    @Transactional
    @Caching(evict = {
        @CacheEvict(value = "products",       allEntries = true),
        @CacheEvict(value = "activeProducts", allEntries = true)
    })
    public ProductResponse setActive(UUID id, boolean active) {
        TShirtProduct product = productRepository.findById(id)
                .orElseThrow(() -> new BusinessException("Product not found"));
        product.setActive(active);
        product = productRepository.save(product);
        return toResponse(product);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "activeProducts")
    public List<ProductResponse> getActiveProducts() {
        return productRepository.findByActiveTrue().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "products")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAllWithCollections().stream()
                .map(this::toResponse)
                .toList();
    }

    // ─── Internals ─────────────────────────────────────────────

    private List<ProductImage> saveImages(TShirtProduct product, List<MultipartFile> images, Integer primaryImageIndex) {
        int max = 8;
        List<MultipartFile> safe = images.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        log.info("[ProductService] saveImages filter: {} non-null out of {} passed", safe.size(), images.size());

        if (safe.size() > max) {
            log.error("[ProductService] Too many images: {} (max is {})", safe.size(), max);
            throw new BusinessException("You can upload up to " + max + " images per product");
        }

        List<ProductImage> saved = new ArrayList<>();
        for (int i = 0; i < safe.size(); i++) {
            MultipartFile file = safe.get(i);
            if (file.isEmpty()) {
                log.info("[ProductService] Image index {} is empty, skipping.", i);
                continue;
            }

            // Validate MIME type, extension and file size
            String contentType = file.getContentType();
            log.info("[ProductService] Image index {}: name={}, contentType={}, size={} bytes", 
                    i, file.getOriginalFilename(), contentType, file.getSize());

            if (contentType == null || !contentType.startsWith("image/")) {
                log.error("[ProductService] Invalid MIME type: {}", contentType);
                throw new BusinessException("Only image files are allowed");
            }
            String originalFilename = file.getOriginalFilename();
            if (originalFilename != null && originalFilename.contains(".")) {
                String ext = originalFilename.substring(originalFilename.lastIndexOf(".") + 1).toLowerCase();
                if (!List.of("jpg", "jpeg", "png", "webp", "gif").contains(ext)) {
                    log.error("[ProductService] Invalid file extension: {}", ext);
                    throw new BusinessException("Invalid image file extension: " + ext);
                }
            }
            if (file.getSize() > 10 * 1024 * 1024) {
                log.error("[ProductService] Image size exceeds limit: {} bytes", file.getSize());
                throw new BusinessException("Image file size exceeds limit of 10MB");
            }

            try {
                // CloudinaryService already logs the required upload messages.
                log.info("[ProductService] Uploading image to Cloudinary...");
                Map<String, String> up = cloudinaryService.uploadImage(file, "products");
                boolean isPrimary = primaryImageIndex != null ? i == primaryImageIndex : i == 0;

                log.info("[ProductService] Uploaded successfully to Cloudinary. Public ID: {}, URL: {}, isPrimary: {}", 
                        up.get("publicId"), up.get("url"), isPrimary);


                ProductImage pi = ProductImage.builder()
                        .product(product)
                        .imageUrl(up.get("url"))
                        .cloudinaryPublicId(up.get("publicId"))
                        .isPrimary(isPrimary)
                        .sortOrder(i)
                        .build();

                saved.add(pi);
            } catch (IOException e) {
                log.error("[ProductService] Exception uploading image to Cloudinary", e);
                throw new BusinessException("Image upload failed: " + e.getMessage());
            }
        }
        return saved;
    }

    private ProductResponse toResponse(TShirtProduct p) {
        BigDecimal effective = p.getDiscountPrice() != null ? p.getDiscountPrice() : p.getBasePrice();

        List<ProductImageResponse> imgs = p.getImages() == null ? List.of() : p.getImages().stream()
                .map(img -> new ProductImageResponse(
                        img.getId(),
                        img.getImageUrl(),
                        img.getCloudinaryPublicId(),
                        img.isPrimary(),
                        img.getSortOrder(),
                        img.getColorHex(),
                        img.getCreatedAt()
                ))
                .sorted(Comparator.comparing(ProductImageResponse::sortOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();

        return new ProductResponse(
                p.getId(),
                p.getProductCode(),
                p.getName(),
                p.getDescription(),
                p.getBrand(),
                p.getCategory(),
                p.getFabricType(),
                p.getGsm(),
                p.getNeckType(),
                p.getSleeveType(),
                p.getMinimumOrderQuantity(),
                p.getBasePrice(),
                p.getDiscountPrice(),
                effective,
                p.isActive(),
                p.getStockQuantity(),
                // Copy the LAZY @ElementCollection bags into real lists while the
                // Hibernate session is still open. With open-in-view=false the session
                // closes when this @Transactional method returns, so handing the raw
                // lazy bags to Jackson would trigger LazyInitializationException -> 500.
                p.getAvailableSizes() == null ? List.of() : new ArrayList<>(p.getAvailableSizes()),
                // backend stores colors as hex strings
                p.getAvailableColors() == null ? List.of() : new ArrayList<>(p.getAvailableColors()),
                p.getPrintTypes() == null ? List.of() : new ArrayList<>(p.getPrintTypes()),
                imgs
        );
    }
}


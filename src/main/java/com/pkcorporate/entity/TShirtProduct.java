package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "tshirt_products")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TShirtProduct extends BaseEntity {

    @Column(nullable = false)
    private String productCode;

    @Column(nullable = false)
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String category; // Round Neck, Polo, Hoodie, etc.
    private String fabricType; // Cotton, Polyester, etc.
    private String gsm; // 160, 180, 200, etc.
    private String neckType;
    private String sleeveType;

    @Column(nullable = false)
    @Builder.Default
    private Integer minimumOrderQuantity = 10;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal basePrice;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    // Admin fields (for catalog management)
    @Column(nullable = true, precision = 10, scale = 2)
    private BigDecimal discountPrice;

    @Column(nullable = false)
    @Builder.Default
    private Integer stockQuantity = 0;

    @Column(nullable = false)
    private String brand;


    @ElementCollection
    @CollectionTable(name = "product_available_colors", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "color_hex")
    @Builder.Default
    private List<String> availableColors = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_available_sizes", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "size_label")
    @Builder.Default
    private List<String> availableSizes = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "product_print_types", joinColumns = @JoinColumn(name = "product_id"))
    @Column(name = "print_type")
    @Builder.Default
    private List<String> printTypes = new ArrayList<>();

    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @Builder.Default
    private List<ProductImage> images = new ArrayList<>();

    public void addImage(ProductImage image) {
        if (image == null) {
            return;
        }
        if (images == null) {
            images = new ArrayList<>();
        }
        images.add(image);
        image.setProduct(this);
    }

    public void removeImage(ProductImage image) {
        if (image == null || images == null) {
            return;
        }
        images.remove(image);
        image.setProduct(null);
    }

    public void setImages(List<ProductImage> images) {
        if (this.images == null) {
            this.images = new ArrayList<>();
        }
        this.images.clear();
        if (images != null) {
            images.forEach(this::addImage);
        }
    }
}

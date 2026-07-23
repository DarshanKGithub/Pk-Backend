package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "product_images")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProductImage extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private TShirtProduct product;

    @Column(nullable = false)
    private String imageUrl;

    private String cloudinaryPublicId;
    private String colorHex;
    private boolean isPrimary;
    private Integer sortOrder;
}

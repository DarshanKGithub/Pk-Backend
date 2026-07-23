package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Entity
@Table(name = "order_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderItem extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private TShirtProduct product;

    // Configurations
    private String colorHex;
    private String colorName;
    private String printType;
    private String embroideryDetails;

    // Size quantities stored as JSON: {"S":10, "M":20, "L":15}
    @Column(columnDefinition = "jsonb")
    private String sizeQuantities;

    @Column(nullable = false)
    private Integer totalQuantity;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalPrice;

    // Customization
    private String designPosition; // Front, Back, Both
    private String customText;
}

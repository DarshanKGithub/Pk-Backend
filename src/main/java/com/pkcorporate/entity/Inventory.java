package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "inventory", indexes = {
    @Index(name = "idx_inventory_active", columnList = "active"),
    @Index(name = "idx_inventory_category", columnList = "category")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Inventory extends BaseEntity {

    @Column(nullable = false)
    private String itemName;

    private String itemCode;
    private String category; // FABRIC, THREAD, PACKAGING, ACCESSORIES

    private String fabricType;
    private String gsm;
    private String colorHex;
    private String colorName;
    private String unit; // meters, kg, pcs

    @Column(nullable = false)
    private Double currentStock;

    @Column(nullable = false)
    private Double minimumStockLevel;

    private Double reorderQuantity;

    @Column(precision = 10, scale = 2)
    private BigDecimal costPerUnit;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;

    private String supplierName;
    private String supplierContact;
    private String warehouseLocation;
    private LocalDateTime lastRestockedAt;
    private String notes;
}

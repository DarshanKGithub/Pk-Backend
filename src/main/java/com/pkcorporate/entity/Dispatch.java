package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "dispatch", indexes = {
    @Index(name = "idx_dispatch_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Dispatch extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, READY, DISPATCHED, DELIVERED

    private String courierName;
    private String trackingNumber;
    private String trackingUrl;

    private String dispatchAddress;
    private String receiverName;
    private String receiverPhone;

    private Integer packageCount;
    private Double totalWeight;

    private LocalDateTime dispatchedAt;
    private LocalDateTime estimatedDelivery;
    private LocalDateTime deliveredAt;

    private String dispatchNotes;
    private String podUrl; // Proof of delivery image URL
}

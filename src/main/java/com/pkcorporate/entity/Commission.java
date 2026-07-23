package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "commissions", indexes = {
    @Index(name = "idx_commission_agent", columnList = "agent_id"),
    @Index(name = "idx_commission_order", columnList = "order_id"),
    @Index(name = "idx_commission_agent_status", columnList = "agent_id, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Commission extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal orderValue;

    @Column(nullable = false)
    private Double commissionRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING"; // PENDING, APPROVED, PAID

    private LocalDate paidDate;
    private String paymentReference;
}

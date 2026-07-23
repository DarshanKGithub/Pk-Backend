package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payments", indexes = {
    @Index(name = "idx_payment_order", columnList = "order_id"),
    @Index(name = "idx_payment_razorpay", columnList = "razorpayPaymentId")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false)
    private String paymentType; // ADVANCE or BALANCE

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false)
    private String paymentMethod; // RAZORPAY, BANK_TRANSFER, CASH, CHEQUE, UPI

    // Razorpay
    private String razorpayPaymentId;
    private String razorpayOrderId;
    private String razorpaySignature;

    // Bank Transfer
    private String transactionId;
    private String bankName;
    private String utrNumber;

    @Column(nullable = false)
    @Builder.Default
    private boolean verified = false;

    private LocalDateTime verifiedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "verified_by")
    private User verifiedBy;

    private String receiptUrl;
    private String notes;
    private String screenshotUrl;
}

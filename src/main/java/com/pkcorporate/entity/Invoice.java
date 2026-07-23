package com.pkcorporate.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "invoices", indexes = {
    @Index(name = "idx_invoice_order", columnList = "order_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Invoice extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Column(nullable = false, unique = true)
    private String invoiceNumber;

    @Column(nullable = false)
    private LocalDate invoiceDate;

    private LocalDate dueDate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subtotal;

    private String gstType; // CGST_SGST or IGST
    private BigDecimal cgstRate;
    private BigDecimal sgstRate;
    private BigDecimal igstRate;

    @Column(precision = 12, scale = 2)
    private BigDecimal cgstAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal sgstAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal igstAmount;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal paidAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal balanceAmount;

    private String invoicePdfUrl;

    @Column(nullable = false)
    @Builder.Default
    private boolean sent = false;
}

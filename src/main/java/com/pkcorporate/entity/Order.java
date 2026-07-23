package com.pkcorporate.entity;

import com.pkcorporate.enums.OrderStatus;
import com.pkcorporate.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.BatchSize;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = {
    @Index(name = "idx_order_number", columnList = "orderNumber", unique = true),
    @Index(name = "idx_order_status", columnList = "status"),
    @Index(name = "idx_order_customer", columnList = "customer_id"),
    @Index(name = "idx_order_agent", columnList = "agent_id"),
    @Index(name = "idx_order_designer", columnList = "designer_id")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String orderNumber;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_id", nullable = false)
    private User agent;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "designer_id")
    private User designer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private PaymentStatus paymentStatus = PaymentStatus.PENDING;

    // Pricing
    @Column(precision = 12, scale = 2)
    private BigDecimal subtotal;

    @Column(precision = 12, scale = 2)
    private BigDecimal gstAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @Column(precision = 12, scale = 2)
    private BigDecimal advanceAmount; // 70%

    @Column(precision = 12, scale = 2)
    private BigDecimal balanceAmount; // 30%

    @Column(precision = 12, scale = 2)
    private BigDecimal paidAmount;

    // GST
    private String gstRate; // e.g. "5%"
    private String customerGstin;
    private Boolean reverseCharge;

    // Notes
    @Column(columnDefinition = "TEXT")
    private String customerNotes;

    @Column(columnDefinition = "TEXT")
    private String internalNotes;

    // Dates
    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime dispatchedAt;
    private LocalDateTime completedAt;

    // Files (stored as URLs)
    // @BatchSize: when Hibernate loads these collections for N orders it batches
    // the IN queries in groups of 20 instead of firing one query per order.
    @ElementCollection
    @CollectionTable(name = "order_logo_files", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "file_url")
    @BatchSize(size = 20)
    @Builder.Default
    private List<String> customerLogoUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "order_reference_files", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "file_url")
    @BatchSize(size = 20)
    @Builder.Default
    private List<String> referenceFileUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "order_mockup_files", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "file_url")
    @BatchSize(size = 20)
    @Builder.Default
    private List<String> mockupFileUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "order_printable_files", joinColumns = @JoinColumn(name = "order_id"))
    @Column(name = "file_url")
    @BatchSize(size = 20)
    @Builder.Default
    private List<String> printableFileUrls = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private List<OrderItem> items = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @BatchSize(size = 20)
    @Builder.Default
    private List<Payment> payments = new ArrayList<>();

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Invoice invoice;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Production production;

    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Dispatch dispatch;

    // Razorpay
    private String razorpayOrderId;
    private String trackingToken; // for customer portal
}

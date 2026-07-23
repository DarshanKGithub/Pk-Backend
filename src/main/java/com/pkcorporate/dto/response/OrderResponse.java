package com.pkcorporate.dto.response;

import com.pkcorporate.enums.OrderStatus;
import com.pkcorporate.enums.PaymentStatus;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private UUID id;
    private String orderNumber;
    private OrderStatus status;
    private PaymentStatus paymentStatus;

    private CustomerInfo customer;
    private AgentInfo agent;
    private DesignerInfo designer;

    private BigDecimal subtotal;
    private BigDecimal gstAmount;
    private BigDecimal totalAmount;
    private BigDecimal advanceAmount;
    private BigDecimal balanceAmount;
    private BigDecimal paidAmount;

    private String gstRate;
    private String customerNotes;
    private String internalNotes;
    private String trackingToken;

    private List<String> customerLogoUrls;
    private List<String> referenceFileUrls;
    private List<String> mockupFileUrls;

    private LocalDateTime expectedDeliveryDate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    private List<OrderItemResponse> items;

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class CustomerInfo {
        private UUID id;
        private String name;
        private String email;
        private String phone;
        private String company;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class AgentInfo {
        private UUID id;
        private String name;
        private String email;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class DesignerInfo {
        private UUID id;
        private String name;
    }

    @Data @Builder @NoArgsConstructor @AllArgsConstructor
    public static class OrderItemResponse {
        private UUID id;
        private String productName;
        private String productCode;
        private String colorHex;
        private String colorName;
        private String printType;
        private String sizeQuantities;
        private Integer totalQuantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
    }
}

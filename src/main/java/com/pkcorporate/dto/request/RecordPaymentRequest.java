package com.pkcorporate.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecordPaymentRequest {
    @NotNull(message = "Order ID is required")
    private UUID orderId;

    @NotNull(message = "Payment Type is required (e.g. ADVANCE or BALANCE)")
    private String paymentType; 

    @NotNull(message = "Payment Method is required")
    private String paymentMethod; // CASH, BANK_TRANSFER, CHEQUE, UPI, CREDIT

    @NotNull(message = "Amount is required")
    private BigDecimal amount;

    private String transactionId;
    private String bankName;
    private String utrNumber;
    private String notes;
    private String screenshotUrl;
}

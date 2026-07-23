package com.pkcorporate.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
public class CreateOrderRequest {

    @NotNull
    private UUID customerId;

    @NotEmpty
    private List<OrderItemRequest> items;

    private String customerNotes;
    private String internalNotes;
    private String expectedDeliveryDate;
    private String customerGstin;

    @Data
    public static class OrderItemRequest {
        @NotNull
        private UUID productId;

        @NotBlank
        private String colorHex;

        private String colorName;

        @NotBlank
        private String printType;

        private String embroideryDetails;
        private String designPosition;
        private String customText;

        @NotEmpty
        private List<SizeQtyRequest> sizeQuantities;

        @Data
        public static class SizeQtyRequest {
            @NotBlank
            private String size;

            @NotNull @Min(0)
            private Integer quantity;
        }
    }
}

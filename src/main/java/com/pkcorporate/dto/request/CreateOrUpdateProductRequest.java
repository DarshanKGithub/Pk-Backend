package com.pkcorporate.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreateOrUpdateProductRequest(
        @NotBlank(message = "productCode is required")
        String productCode,

        @NotBlank(message = "name is required")
        String name,

        String description,

        @NotBlank(message = "category is required")
        String category,

        @NotBlank(message = "fabricType is required")
        String fabricType,

        @NotBlank(message = "gsm is required")
        String gsm,

        @NotBlank(message = "neckType is required")
        String neckType,

        @NotBlank(message = "sleeveType is required")
        String sleeveType,

        @NotNull(message = "basePrice is required")
        @DecimalMin(value = "0.0", inclusive = false, message = "basePrice must be > 0")
        BigDecimal basePrice,

        BigDecimal discountPrice,

        @NotNull(message = "stockQuantity is required")
        @DecimalMin(value = "0.0", inclusive = true)
        Integer stockQuantity,

        @NotBlank(message = "brand is required")
        String brand,

        @NotEmpty(message = "availableSizes must not be empty")
        List<@NotBlank String> availableSizes,

        @NotEmpty(message = "availableColors must not be empty")
        List<@NotBlank String> availableColors,

        @NotEmpty(message = "printTypes must not be empty")
        List<@NotBlank String> printTypes,

        Integer minimumOrderQuantity,

        Boolean active
) {
    public CreateOrUpdateProductRequest {
        if (minimumOrderQuantity != null && minimumOrderQuantity <= 0) {
            throw new IllegalArgumentException("minimumOrderQuantity must be > 0");
        }
        if (discountPrice != null && discountPrice.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("discountPrice must be >= 0");
        }
    }
}


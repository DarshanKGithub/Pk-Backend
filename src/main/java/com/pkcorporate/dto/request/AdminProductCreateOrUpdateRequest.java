package com.pkcorporate.dto.request;

import jakarta.validation.constraints.DecimalMin;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.List;


/**
 * Admin request contract for product CRUD + catalog management.
 */
public record AdminProductCreateOrUpdateRequest(
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

        @DecimalMin(value = "0.0", inclusive = true, message = "discountPrice must be >= 0")
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

        Boolean active,

        /**
         * Primary image index (0-based). Optional; if not provided, first image becomes primary.
         */
        Integer primaryImageIndex
) {}


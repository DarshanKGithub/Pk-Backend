package com.pkcorporate.dto.response;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
        UUID id,
        String productCode,
        String name,
        String description,
        String brand,
        String category,
        String fabric,
        String gsm,
        String neckType,
        String sleeveType,
        Integer minimumOrderQuantity,
        BigDecimal basePrice,
        BigDecimal discountPrice,
        BigDecimal effectivePrice,
        boolean active,
        Integer stockQuantity,
        List<String> availableSizes,
        List<String> availableColors,
        List<String> printTypes,
        List<ProductImageResponse> images
) {
}


package com.pkcorporate.dto.response;

import java.time.LocalDateTime;
import java.util.UUID;

public record ProductImageResponse(
        UUID id,
        String imageUrl,
        String cloudinaryPublicId,
        boolean isPrimary,
        Integer sortOrder,
        String colorHex,
        LocalDateTime createdAt
) {
}


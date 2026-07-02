package com.example.cabbagemarket10.domain.item.dto.response;

public record ItemDetailImageResponse(
        Long imageId,
        String imageUrl,
        Integer sortOrder,
        Boolean isThumbnail
) {
}

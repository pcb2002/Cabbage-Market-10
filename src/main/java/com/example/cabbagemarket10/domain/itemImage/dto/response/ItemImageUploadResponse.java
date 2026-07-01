package com.example.cabbagemarket10.domain.itemImage.dto.response;

import java.util.List;

public record ItemImageUploadResponse(
        Long itemId,
        List<ImageInfo> uploadedImages
) {
    public static ItemImageUploadResponse of(Long itemId, List<ImageInfo> uploadedImages) {
        return new ItemImageUploadResponse(itemId, uploadedImages);
    }

    public record ImageInfo(
            Long imageId,
            String imageUrl,
            int sortOrder,
            boolean isThumbnail
    ) {}
}
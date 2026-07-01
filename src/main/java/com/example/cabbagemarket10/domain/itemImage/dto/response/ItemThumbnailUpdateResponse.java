package com.example.cabbagemarket10.domain.itemImage.dto.response;

public record ItemThumbnailUpdateResponse(
        Long itemId,
        Long thumbnailImageId,
        String thumbnailUrl
) {
    public static ItemThumbnailUpdateResponse of(Long itemId, Long thumbnailImageId, String thumbnailUrl) {
        return new ItemThumbnailUpdateResponse(itemId, thumbnailImageId, thumbnailUrl);
    }
}
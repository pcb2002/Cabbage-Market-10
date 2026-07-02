package com.example.cabbagemarket10.domain.item.dto.response;

public record ItemLikeToggleResponse(
        Long itemId,
        boolean liked,
        Long likeCount
) {
}

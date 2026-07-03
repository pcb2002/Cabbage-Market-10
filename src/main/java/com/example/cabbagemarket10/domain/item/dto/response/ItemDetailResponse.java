package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import java.time.LocalDateTime;
import java.util.List;

public record ItemDetailResponse(
        Long itemId,
        Long sellerId,
        String title,
        String description,
        Long initialPrice,
        Long currentBid,
        TradeStatus tradeStatus,
        LocalDateTime closeDate,
        Long viewCount,
        Long likeCount,
        Long inquiryCount,
        List<ItemDetailImageResponse> images
) {
    public ItemDetailResponse(
            Long itemId,
            Long sellerId,
            String title,
            String description,
            Long initialPrice,
            Long currentBid,
            TradeStatus tradeStatus,
            LocalDateTime closeDate,
            Long viewCount,
            Long likeCount,
            Long inquiryCount
    ) {
        this(itemId, sellerId, title, description, initialPrice, currentBid, tradeStatus, closeDate,
                viewCount, likeCount, inquiryCount, List.of());
    }

    public ItemDetailResponse withImages(List<ItemDetailImageResponse> images) {
        return new ItemDetailResponse(itemId, sellerId, title, description, initialPrice, currentBid, tradeStatus, closeDate,
                viewCount, likeCount, inquiryCount, images);
    }
}

package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import java.time.LocalDateTime;

public record MyLikedItemResponse(
        Long itemId,
        String title,
        Long initialPrice,
        Long currentBid,
        TradeStatus tradeStatus,
        LocalDateTime closeDate,
        TradeType tradeType,
        ConditionType conditionType,
        Long likeCount,
        boolean likedByMe,
        String thumbnailUrl,
        Long categoryId,
        LocalDateTime createdAt
) {
}

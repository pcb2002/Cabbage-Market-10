package com.example.cabbagemarket10.domain.search.dto.response;

import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import java.time.LocalDateTime;
import java.io.Serializable;

public record SearchItemResponse(
        Long itemId,
        String thumbnailUrl,
        String categoryName,
        String title,
        TradeType tradeType,
        Long initialPrice,
        Long currentBid,
        TradeStatus tradeStatus,
        Long likeCount,
        LocalDateTime createdAt
) implements Serializable {
}

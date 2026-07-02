package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import java.time.LocalDateTime;

public record ItemListItemResponse(
        Long itemId,
        String thumbnailUrl,
        String categoryName,
        String title,
        Long initialPrice,
        Long currentBid, // AuctionStatus와 조인하여 가져올 데이터
        TradeStatus tradeStatus,
        TradeType tradeType,
        ConditionType conditionType,
        LocalDateTime closeDate,
        Long likeCount,
        LocalDateTime createdAt
) {}

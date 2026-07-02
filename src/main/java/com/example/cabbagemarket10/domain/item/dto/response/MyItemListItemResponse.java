package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import java.time.LocalDateTime;

public record MyItemListItemResponse(
        Long itemId,
        String title,
        Long initialPrice,
        Long currentBid, // AuctionStatus와 조인하여 가져올 데이터
        TradeStatus tradeStatus,
        LocalDateTime closeDate,
        Boolean isDraft,
        TradeType tradeType,
        ConditionType conditionType,
        Long likeCount,
        String thumbnailUrl, // 대표 이미지 서브쿼리로 가져올 데이터
        Long categoryId,
        LocalDateTime createdAt
) {}

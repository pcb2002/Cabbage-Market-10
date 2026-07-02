package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import java.time.LocalDateTime;

public record ItemListItemResponse(
        Long itemId,
        String thumbnailUrl,
        String title,
        Long initialPrice,
        Long currentBid, // AuctionStatus와 조인하여 가져올 데이터
        TradeStatus tradeStatus,
        LocalDateTime closeDate
) {}

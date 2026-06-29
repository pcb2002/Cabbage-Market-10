package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import java.time.LocalDateTime;

public record ItemDetailResponse(
        Long itemId,
        String title,
        String description,
        Long initialPrice,
        Long currentBid,
        TradeStatus tradeStatus,
        LocalDateTime closeDate,
        Long viewCount,
        Long likeCount,
        Long inquiryCount
) {}
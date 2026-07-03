package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import java.time.LocalDateTime;

public record ItemStatusUpdateResponse(
        Long itemId,
        TradeStatus tradeStatus,
        Long buyerId,
        LocalDateTime updatedAt
) {
}

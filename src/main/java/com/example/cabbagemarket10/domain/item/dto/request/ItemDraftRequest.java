package com.example.cabbagemarket10.domain.item.dto.request;

import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeType;

import java.time.LocalDateTime;

// 임시저장용 DTO: 모든 필드는 Null을 허용합니다.
public record ItemDraftRequest(
        Long categoryId,
        String title,
        TradeType tradeType,
        ConditionType conditionType,
        String description,
        Long initialPrice,
        LocalDateTime closeDate
) {
}
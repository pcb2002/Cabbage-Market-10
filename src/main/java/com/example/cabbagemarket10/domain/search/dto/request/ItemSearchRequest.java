package com.example.cabbagemarket10.domain.search.dto.request;

import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

public record ItemSearchRequest(
        @Size(max = 100, message = "검색어는 최대 100자까지 입력할 수 있습니다.")
        String keyword,
        Long categoryId,
        TradeStatus tradeStatus,            // ON_SALE / RESERVED / SOLD_OUT
        TradeType tradeType,                // DIRECT / AUCTION
        ConditionType conditionType,        // NEW / USED
        Boolean likedOnly,

        @PositiveOrZero(message = "최소 가격은 0 이상이어야 합니다.")
        Long minPrice,

        @PositiveOrZero(message = "최대 가격은 0 이상이어야 합니다.")
        Long maxPrice
) {
}

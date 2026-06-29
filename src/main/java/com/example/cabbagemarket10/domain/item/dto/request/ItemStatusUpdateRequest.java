package com.example.cabbagemarket10.domain.item.dto.request;

import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import jakarta.validation.constraints.NotNull;

public record ItemStatusUpdateRequest(
        @NotNull(message = "판매 상태는 필수입니다.")
        TradeStatus tradeStatus
) {
}

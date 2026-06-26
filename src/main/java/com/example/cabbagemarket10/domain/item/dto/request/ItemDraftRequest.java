package com.example.cabbagemarket10.domain.item.dto.request;

import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ItemDraftRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @NotBlank(message = "제목은 필수입니다.")
        String title,

        @NotNull(message = "거래 방식은 필수입니다.")
        TradeType tradeType,

        @NotNull(message = "상품 상태는 필수입니다.")
        ConditionType conditionType,

        @NotBlank(message = "상품 설명은 필수입니다.")
        String description,

        @NotNull(message = "시작가는 필수입니다.")
        Long initialPrice,

        LocalDateTime closeDate
) {
}

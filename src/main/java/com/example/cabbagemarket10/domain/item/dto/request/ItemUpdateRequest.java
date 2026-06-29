package com.example.cabbagemarket10.domain.item.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

public record ItemUpdateRequest(
        @NotNull(message = "카테고리 ID는 필수입니다.")
        Long categoryId,

        @NotBlank(message = "상품명은 필수입니다.")
        String title,

        @NotBlank(message = "상세 설명은 필수입니다.")
        String description,

        @NotNull(message = "시작가는 필수입니다.")
        @Min(value = 0, message = "시작가는 0 이상이어야 합니다.")
        Long initialPrice,

        @NotNull(message = "경매 종료일은 필수입니다.")
        @Future(message = "경매 종료일은 현재 시간 이후여야 합니다.")
        LocalDateTime closeDate
) {}
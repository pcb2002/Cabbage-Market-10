package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.item.entity.Item;
import java.time.LocalDateTime;

public record ItemDraftResponse(
        Long itemId,
        LocalDateTime updatedAt
) {
    public static ItemDraftResponse from(Item item) {
        return new ItemDraftResponse(
                item.getId(),
                item.getUpdatedAt() // BaseEntity에서 상속받은 필드
        );
    }
}
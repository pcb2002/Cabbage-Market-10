package com.example.cabbagemarket10.domain.item.dto.response;

import java.time.LocalDateTime;

public record ItemUpdateResponse(
        Long itemId,
        LocalDateTime updatedAt
) {}
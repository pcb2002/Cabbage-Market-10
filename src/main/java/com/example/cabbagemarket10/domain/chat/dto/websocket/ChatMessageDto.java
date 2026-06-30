package com.example.cabbagemarket10.domain.chat.dto.websocket;

import jakarta.validation.constraints.NotBlank;

public record ChatMessageDto(
        long roomId,
        long senderId,
        @NotBlank
        String Content
) {
}

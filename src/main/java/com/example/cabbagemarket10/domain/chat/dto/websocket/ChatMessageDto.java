package com.example.cabbagemarket10.domain.chat.dto.websocket;

import com.example.cabbagemarket10.domain.chat.entity.MessageType;
import jakarta.validation.constraints.NotBlank;

public record ChatMessageDto(
        @NotBlank
        String content,
        @NotBlank
        MessageType contentType
) {
}

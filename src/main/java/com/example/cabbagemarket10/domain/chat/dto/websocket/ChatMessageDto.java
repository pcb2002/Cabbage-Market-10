package com.example.cabbagemarket10.domain.chat.dto.websocket;

import com.example.cabbagemarket10.domain.chat.entity.MessageType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record ChatMessageDto(
        @NotBlank
        String content,
        @NotNull
        MessageType contentType
) {
}

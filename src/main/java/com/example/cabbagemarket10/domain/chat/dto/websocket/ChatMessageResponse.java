package com.example.cabbagemarket10.domain.chat.dto.websocket;

import com.example.cabbagemarket10.domain.chat.entity.ChatMessage;
import com.example.cabbagemarket10.domain.client.entity.Client;

import java.time.LocalDateTime;

public record ChatMessageResponse(
        long messageId,
        String content,
        long senderId,
        String senderName,
        LocalDateTime createdAt
) {

    public static ChatMessageResponse from (ChatMessage message) {
        Client sender = message.getSender();
        return new ChatMessageResponse(
                message.getId(),
                message.getContent(),
                sender.getId(),
                sender.getName(),
                message.getCreatedAt()
        );
    }
}

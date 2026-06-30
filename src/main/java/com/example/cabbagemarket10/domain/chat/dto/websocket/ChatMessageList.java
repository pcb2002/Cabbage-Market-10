package com.example.cabbagemarket10.domain.chat.dto.websocket;

import com.example.cabbagemarket10.domain.chat.entity.ChatMessage;
import org.springframework.data.domain.Page;

import java.util.List;

public record ChatMessageList(
        List<ChatMessageResponse> content,
        int page,
        int size,
        long totalElements,
        int totalPages
) {
    public static ChatMessageList from(Page<ChatMessage> messages) {
        return new ChatMessageList(
                messages.getContent().stream()
                .map(ChatMessageResponse::from)
                .toList(),
                messages.getNumber(),
                messages.getSize(),
                messages.getTotalElements(),
                messages.getTotalPages());
    }
}

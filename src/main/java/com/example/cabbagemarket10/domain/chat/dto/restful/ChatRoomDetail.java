package com.example.cabbagemarket10.domain.chat.dto.restful;

import com.example.cabbagemarket10.domain.chat.entity.ChatRoom;

import java.time.LocalDateTime;

public record ChatRoomDetail(
        String id,
        String itemName,
        LocalDateTime date
) {
    public static ChatRoomDetail from(ChatRoom chatRoom) {
        return new ChatRoomDetail(
                chatRoom.getId(),
                chatRoom.getItem().getTitle(),
                chatRoom.getLastMessageAt()
        );
    }
}

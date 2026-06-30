package com.example.cabbagemarket10.domain.chat.dto.restful;

import com.example.cabbagemarket10.domain.chat.entity.ChatRoom;

public record RoomCreate(String roomId) {

    public static RoomCreate from(ChatRoom chatRoom) {
        return new RoomCreate(chatRoom.getId());
    }
}

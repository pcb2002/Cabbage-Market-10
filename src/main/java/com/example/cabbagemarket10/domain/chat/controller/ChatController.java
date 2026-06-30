package com.example.cabbagemarket10.domain.chat.controller;

import com.example.cabbagemarket10.domain.chat.dto.restful.RoomCreate;
import com.example.cabbagemarket10.domain.chat.service.ChatService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items/{itemId}/chat-rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<CommonResponse<RoomCreate>> createChatRoom(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient
    ) {
        RoomCreate resBody = chatService.createRoom(authenticatedClient.clientId(), itemId);
        return CommonResponse.success(HttpStatus.CREATED, resBody).toResponseEntity();
    }

}

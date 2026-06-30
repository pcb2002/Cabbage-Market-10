package com.example.cabbagemarket10.domain.chat.controller;

import com.example.cabbagemarket10.domain.chat.dto.restful.RoomCreate;
import com.example.cabbagemarket10.domain.chat.service.ChatService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chat-rooms")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/{itemId}")
    public ResponseEntity<CommonResponse<RoomCreate>> createChatRoom(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient
    ) {
        RoomCreate resBody = chatService.createRoom(authenticatedClient.clientId(), itemId);
        return CommonResponse.success(HttpStatus.CREATED, resBody).toResponseEntity();
    }

    @DeleteMapping("/{messageId}")
    public ResponseEntity<CommonResponse<Void>> deleteChatMessage(
            @PathVariable Long messageId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient
    ) {
        chatService.deleteMessage(messageId, authenticatedClient.clientId());
        return CommonResponse.success(HttpStatus.NO_CONTENT).toResponseEntity();
    }

}

package com.example.cabbagemarket10.domain.chat.controller;

import com.example.cabbagemarket10.domain.chat.dto.restful.ChatRoomDetail;
import com.example.cabbagemarket10.domain.chat.dto.restful.RoomCreate;
import com.example.cabbagemarket10.domain.chat.dto.websocket.ChatMessageList;
import com.example.cabbagemarket10.domain.chat.service.ChatService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.common.PageResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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

    @GetMapping("/{chatRoomId}/messages")
    public ResponseEntity<CommonResponse<ChatMessageList>> getRecentChatMessages(
            @PathVariable String chatRoomId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            @PageableDefault(size = 50) Pageable pageable
    ) {
        ChatMessageList resBody = chatService.getRecentMessages(chatRoomId, authenticatedClient.clientId(), pageable);
        return CommonResponse.success(HttpStatus.OK, resBody).toResponseEntity();
    }

    @GetMapping("/my")
    public ResponseEntity<CommonResponse<PageResponse<ChatRoomDetail>>> getMyChatRooms(
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        PageResponse<ChatRoomDetail> resBody = chatService.getMyChatRoom(authenticatedClient.clientId(), pageable);
        return CommonResponse.success(HttpStatus.OK, resBody).toResponseEntity();
    }
}

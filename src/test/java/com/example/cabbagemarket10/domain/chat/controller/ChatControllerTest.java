package com.example.cabbagemarket10.domain.chat.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.chat.dto.restful.ChatRoomDetail;
import com.example.cabbagemarket10.domain.chat.dto.websocket.ChatMessageList;
import com.example.cabbagemarket10.domain.chat.service.ChatService;
import com.example.cabbagemarket10.global.common.response.CommonResponse;
import com.example.cabbagemarket10.global.common.response.PageResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class ChatControllerTest {

    @Mock
    private ChatService chatService;

    @InjectMocks
    private ChatController chatController;

    @DisplayName("최근 채팅 메시지 목록 조회 요청을 서비스에 위임한다")
    @Test
    void 최근_채팅_메시지_목록_조회_요청을_서비스에_위임한다() {
        AuthenticatedClient authenticatedClient = new AuthenticatedClient(1L, "client@example.com");
        Pageable pageable = PageRequest.of(0, 50);
        ChatMessageList serviceResponse = new ChatMessageList(List.of(), 0, 50, 0, 0);
        given(chatService.getRecentMessages("room-1", 1L, pageable)).willReturn(serviceResponse);

        ResponseEntity<CommonResponse<ChatMessageList>> response =
                chatController.getRecentChatMessages("room-1", authenticatedClient, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(chatService).getRecentMessages("room-1", 1L, pageable);
    }

    @DisplayName("내 채팅방 목록 조회 요청을 서비스에 위임한다")
    @Test
    void 내_채팅방_목록_조회_요청을_서비스에_위임한다() {
        AuthenticatedClient authenticatedClient = new AuthenticatedClient(1L, "client@example.com");
        Pageable pageable = PageRequest.of(0, 20);
        PageResponse<ChatRoomDetail> serviceResponse = PageResponse.from(
                new PageImpl<>(List.of(), pageable, 0));
        given(chatService.getMyChatRoom(1L, pageable)).willReturn(serviceResponse);

        ResponseEntity<CommonResponse<PageResponse<ChatRoomDetail>>> response =
                chatController.getMyChatRooms(authenticatedClient, pageable);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getData()).isSameAs(serviceResponse);
        verify(chatService).getMyChatRoom(1L, pageable);
    }
}

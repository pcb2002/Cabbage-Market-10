package com.example.cabbagemarket10.domain.chat.controller;

import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.chat.dto.websocket.ChatMessageDto;
import com.example.cabbagemarket10.domain.chat.entity.MessageType;
import com.example.cabbagemarket10.domain.chat.service.ChatService;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock
    private ChatService chatService;

    @Mock
    private SimpMessagingTemplate simpMessagingTemplate;

    @InjectMocks
    private ChatWebSocketController chatWebSocketController;

    @DisplayName("메시지를 전송하면 서비스 저장 후 채팅방 구독 경로로 발행한다")
    @Test
    void 메시지를_전송하면_서비스_저장_후_채팅방_구독_경로로_발행한다() {
        ChatMessageDto request = new ChatMessageDto("안녕하세요", MessageType.TEXT);
        AuthenticatedClient principal = new AuthenticatedClient(1L, "sender@example.com");

        chatWebSocketController.sendMessage("room-1", request, principal);

        verify(chatService).sendMessage("room-1", request, principal);
        verify(simpMessagingTemplate).convertAndSend("/subroom-1/messages", request);
    }
}

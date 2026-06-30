package com.example.cabbagemarket10.domain.chat.controller;

import com.example.cabbagemarket10.domain.chat.dto.websocket.ChatMessageDto;
import com.example.cabbagemarket10.domain.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate simpMessagingTemplate;


    @MessageMapping("/{roomId}/messages")
    public void sendMessage(
            @DestinationVariable String roomId,
            @Valid ChatMessageDto chatMessageDto,
            Principal principal
    ) {

        chatService.sendMessage(roomId, chatMessageDto, principal);
        simpMessagingTemplate.convertAndSend("/sub/" + roomId + "/messages", chatMessageDto);
    }

}

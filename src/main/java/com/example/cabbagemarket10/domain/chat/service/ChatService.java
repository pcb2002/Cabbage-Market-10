package com.example.cabbagemarket10.domain.chat.service;

import com.example.cabbagemarket10.domain.chat.dto.restful.RoomCreate;
import com.example.cabbagemarket10.domain.chat.dto.websocket.ChatMessageDto;
import com.example.cabbagemarket10.domain.chat.entity.ChatMessage;
import com.example.cabbagemarket10.domain.chat.entity.ChatRoom;
import com.example.cabbagemarket10.domain.chat.repository.ChatMessageRepository;
import com.example.cabbagemarket10.domain.chat.repository.ChatRoomRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.Principal;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ClientRepository clientRepository;
    private final ItemRepository itemRepository;
    private final ChatMessageRepository chatMessageRepository;

    public RoomCreate createRoom(long buyerId, long itemId) {

        Client buyer = clientRepository.findById(buyerId).orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        ChatRoom chatRoom = new ChatRoom(item, buyer);
        chatRoom = chatRoomRepository.save(chatRoom);

        return RoomCreate.from(chatRoom);
    }

    public void sendMessage(String roomId, ChatMessageDto chatMessageDto, Principal principal) {
        Client sender = clientRepository.findById(AuthenticatedClient.fromPrincipal(principal))
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        ChatRoom chatRoom = chatRoomRepository.findById(roomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));

        ChatMessage chatMessage =ChatMessage.builder()
                .chatRoom(chatRoom)
                .sender(sender)
                .content(chatMessageDto.content())
                .messageType(chatMessageDto.contentType())
                .build();

        chatMessageRepository.save(chatMessage);
    }

}

package com.example.cabbagemarket10.domain.chat.service;

import com.example.cabbagemarket10.domain.chat.dto.restful.ChatRoomDetail;
import com.example.cabbagemarket10.domain.chat.dto.restful.RoomCreate;
import com.example.cabbagemarket10.domain.chat.dto.websocket.ChatMessageDto;
import com.example.cabbagemarket10.domain.chat.dto.websocket.ChatMessageList;
import com.example.cabbagemarket10.domain.chat.entity.ChatMessage;
import com.example.cabbagemarket10.domain.chat.entity.ChatRoom;
import com.example.cabbagemarket10.domain.chat.repository.ChatMessageRepository;
import com.example.cabbagemarket10.domain.chat.repository.ChatRoomRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.common.PageResponse;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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

    public void deleteMessage(Long messageId, Long clientId) {
        ChatMessage chatMessage = chatMessageRepository.findById(messageId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_FOUND));

        if(chatMessage.isNotPublisher(clientId)) {
            throw new BusinessException(ErrorCode.CHAT_MESSAGE_NOT_PUBLISHER);
        }

        chatMessageRepository.delete(chatMessage);
    }

    public ChatMessageList getRecentMessages(
            String chatRoomId,
            Long clientId,
            Pageable pageable) {

        ChatRoom room  = chatRoomRepository.findById(chatRoomId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CHAT_ROOM_NOT_FOUND));
        room.inspectClientAsParticipant(clientId);

        Pageable descPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("createdAt").descending());
        Page<ChatMessage> chatMessagesPage = chatMessageRepository.findByChatRoomId(chatRoomId, descPageable);

        return ChatMessageList.from(chatMessagesPage);
    }

    @Transactional(readOnly = true)
    public PageResponse<ChatRoomDetail> getMyChatRoom(Long clientId, Pageable pageable) {
        Pageable searchPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by("lastMessageAt").descending());

        Page<ChatRoomDetail> chatRoomList = chatRoomRepository.findByClientId(clientId, searchPageable);
        return PageResponse.from(chatRoomList);
    }
}

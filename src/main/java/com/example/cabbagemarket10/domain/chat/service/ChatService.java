package com.example.cabbagemarket10.domain.chat.service;

import com.example.cabbagemarket10.domain.chat.dto.restful.RoomCreate;
import com.example.cabbagemarket10.domain.chat.entity.ChatRoom;
import com.example.cabbagemarket10.domain.chat.repository.ChatRoomRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ChatService {

    private final ChatRoomRepository chatRoomRepository;
    private final ClientRepository clientRepository;
    private final ItemRepository itemRepository;

    public RoomCreate createRoom(long buyerId, long itemId) {

        Client buyer = clientRepository.findById(buyerId).orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));
        Item item = itemRepository.findById(itemId).orElseThrow(() -> new BusinessException(ErrorCode.ITEM_NOT_FOUND));

        ChatRoom chatRoom = new ChatRoom(item, buyer);
        chatRoom = chatRoomRepository.save(chatRoom);

        return RoomCreate.from(chatRoom);
    }
}

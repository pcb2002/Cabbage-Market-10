package com.example.cabbagemarket10.domain.itemLike.service;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.dto.response.ItemLikeToggleResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemLike.entity.ItemLike;
import com.example.cabbagemarket10.domain.itemLike.repository.ItemLikeRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemLikeService {

    private final ItemLikeRepository itemLikeRepository;
    private final ItemRepository itemRepository;
    private final ItemService itemService;
    private final ClientRepository clientRepository;

    @Transactional
    public ItemLikeToggleResponse toggle(Long itemId, Long clientId) {
        Item item = itemService.getItem(itemId);
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        return itemLikeRepository.findByClient_IdAndItem_Id(clientId, itemId)
                .map(existingLike -> cancelLike(item, existingLike))
                .orElseGet(() -> addLike(item, client));
    }

    private ItemLikeToggleResponse addLike(Item item, Client client) {
        itemLikeRepository.save(ItemLike.builder()
                .client(client)
                .item(item)
                .build());
        itemRepository.incrementLikeCount(item.getId());
        return new ItemLikeToggleResponse(item.getId(), true, itemRepository.findLikeCountById(item.getId()));
    }

    private ItemLikeToggleResponse cancelLike(Item item, ItemLike existingLike) {
        itemLikeRepository.delete(existingLike);
        itemRepository.decrementLikeCount(item.getId());
        return new ItemLikeToggleResponse(item.getId(), false, itemRepository.findLikeCountById(item.getId()));
    }
}

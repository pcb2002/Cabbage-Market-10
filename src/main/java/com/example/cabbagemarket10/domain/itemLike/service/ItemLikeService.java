package com.example.cabbagemarket10.domain.itemLike.service;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.itemLike.entity.ItemLike;
import com.example.cabbagemarket10.domain.itemLike.repository.ItemLikeRepository;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemLikeService {

    private final ItemLikeRepository itemLikeRepository;

    public Optional<ItemLike> findByClientIdAndItemId(Long clientId, Long itemId) {
        return itemLikeRepository.findByClient_IdAndItem_Id(clientId, itemId);
    }

    public ItemLike save(Client client, Item item) {
        return itemLikeRepository.save(ItemLike.builder()
                .client(client)
                .item(item)
                .build());
    }

    public void delete(ItemLike itemLike) {
        itemLikeRepository.delete(itemLike);
    }
}

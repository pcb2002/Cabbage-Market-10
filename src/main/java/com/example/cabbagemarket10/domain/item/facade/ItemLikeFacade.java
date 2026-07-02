package com.example.cabbagemarket10.domain.item.facade;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.dto.response.ItemLikeToggleResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemLike.entity.ItemLike;
import com.example.cabbagemarket10.domain.itemLike.service.ItemLikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemLikeFacade {

    private final ItemService itemService;
    private final ClientService clientService;
    private final ItemLikeService itemLikeService;

    @Transactional
    public ItemLikeToggleResponse toggle(Long itemId, Long clientId) {
        Item item = itemService.getItemForUpdate(itemId);
        Client client = clientService.getClient(clientId);

        return itemLikeService.findByClientIdAndItemId(clientId, itemId)
                .map(existingLike -> cancelLike(item, existingLike))
                .orElseGet(() -> addLike(item, client));
    }

    private ItemLikeToggleResponse addLike(Item item, Client client) {
        itemLikeService.save(client, item);
        itemService.incrementLikeCount(item.getId());
        return new ItemLikeToggleResponse(item.getId(), true, itemService.getLikeCount(item.getId()));
    }

    private ItemLikeToggleResponse cancelLike(Item item, ItemLike existingLike) {
        itemLikeService.delete(existingLike);
        itemService.decrementLikeCount(item.getId());
        return new ItemLikeToggleResponse(item.getId(), false, itemService.getLikeCount(item.getId()));
    }
}

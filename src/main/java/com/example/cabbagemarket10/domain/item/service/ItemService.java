package com.example.cabbagemarket10.domain.item.service;

import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemDraftRequest;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ItemService {

    private final ItemRepository itemRepository;

    public Item saveItem(Client seller, Category category, ItemCreateRequest request) {
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .initialPrice(request.initialPrice())
                .tradeType(request.tradeType())
                .conditionType(request.conditionType())
                .isDraft(false)
                .tradeStatus(TradeStatus.ON_SALE)
                .build();
        return itemRepository.save(item);
    }

    public Item saveItemDraft(Client seller, Category category, ItemDraftRequest request) {
        Item item = Item.builder()
                .seller(seller)
                .category(category)
                .title(request.title())
                .description(request.description())
                .initialPrice(request.initialPrice())
                .tradeType(request.tradeType())
                .conditionType(request.conditionType())
                .isDraft(true)
                .tradeStatus(TradeStatus.ON_SALE)
                .build();
        return itemRepository.save(item);
    }
}
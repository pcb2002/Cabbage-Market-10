package com.example.cabbagemarket10.application.facade;

import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.service.CategoryService;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemDraftRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDraftResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ItemFacade {

    private final ClientService clientService;
    private final CategoryService categoryService;
    private final ItemService itemService;
    private final AuctionStatusService auctionStatusService;

    @Transactional
    public Long createItem(Long sellerId, ItemCreateRequest request) {
        Client seller = clientService.getClient(sellerId);
        Category category = categoryService.getCategory(request.categoryId());

        Item item = itemService.saveItem(seller, category, request);
        auctionStatusService.createAuctionStatus(item, request.initialPrice(), request.closeDate());

        return item.getId();
    }

    @Transactional
    public ItemDraftResponse createItemDraft(Long sellerId, ItemDraftRequest request) {
        Client seller = clientService.getClient(sellerId);
        Category category = categoryService.getCategory(request.categoryId());

        Item item = itemService.saveItemDraft(seller, category, request);
        if (isAuctionDraftReady(request)) {
            auctionStatusService.createAuctionStatus(item, request.initialPrice(), request.closeDate());
        }

        return ItemDraftResponse.from(item);
    }

    private boolean isAuctionDraftReady(ItemDraftRequest request) {
        return request.tradeType() == TradeType.AUCTION
                && request.initialPrice() != null
                && request.closeDate() != null;
    }
}

package com.example.cabbagemarket10.application.facade;

import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.service.CategoryService;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemDraftRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemUpdateRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDraftResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemPublishResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemUpdateResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemImage.repository.ItemImageRepository;
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
    private final ItemImageRepository itemImageRepository;

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

    @Transactional
    public ItemPublishResponse publishItem(Long itemId, Long clientId) {
        // 1. 상품 조회 및 판매자 검증
        Item item = itemService.getItemValidatingAuthor(itemId, clientId);

        // 2. 경매 상품일 경우 추가 검증
        if (item.isAuction()) {
            auctionStatusService.validateForPublish(itemId);
        }

        // 3. 게시 상태 전환
        item.publish();

        return new ItemPublishResponse(item.getId(), item.getUpdatedAt());
    }

    @Transactional
    public ItemUpdateResponse updateItem(Long itemId, Long clientId, ItemUpdateRequest request) {
        Item item = itemService.getItemValidatingAuthor(itemId, clientId);
        item.validateUpdatable();

        Category category = categoryService.getCategory(request.categoryId());

        if (item.isAuction()) {
            auctionStatusService.syncDraftAuctionStatus(item, request.initialPrice(), request.closeDate());
        }

        item.updateInfo(category, request.title(), request.description(), request.initialPrice());
        itemService.flush();

        return new ItemUpdateResponse(item.getId(), item.getUpdatedAt());
    }

    @Transactional
    public void deleteItem(Long itemId, Long clientId) {
        Item item = itemService.getItemValidatingAuthor(itemId, clientId);

        if (Boolean.TRUE.equals(item.getIsDraft())) {
            auctionStatusService.deleteByItemId(itemId);
            itemImageRepository.deleteByItemId(itemId);
            itemService.hardDeleteById(itemId);
            return;
        }

        itemService.softDelete(item);
    }
}

package com.example.cabbagemarket10.domain.item.facade;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.service.CategoryService;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemDraftRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemStatusUpdateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemUpdateRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemCreateResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDraftResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemPublishResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemStatusUpdateResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemUpdateResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemImage.repository.ItemImageRepository;
import com.example.cabbagemarket10.global.config.cache.SearchCacheEvictionService;
import com.example.cabbagemarket10.domain.itemImage.service.ItemImageService;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
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
    private final SearchCacheEvictionService searchCacheEvictionService;
    private final ItemImageService itemImageService;

    @Transactional
    public ItemCreateResponse createItem(Long sellerId, ItemCreateRequest request) {
        Client seller = clientService.getClient(sellerId);
        Category category = categoryService.getCategory(request.categoryId());

        Item item = itemService.saveItem(seller, category, request);
        auctionStatusService.createAuctionStatus(item, request.initialPrice(), request.closeDate());
        searchCacheEvictionService.evictItemSearchV2AfterCommit();
        AuctionStatus auctionStatus = item.isAuction()
                ? auctionStatusService.createAuctionStatus(item, request.initialPrice(), request.closeDate())
                : null;

        return ItemCreateResponse.of(item, auctionStatus);
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
        Item item = itemService.getItemValidatingAuthor(itemId, clientId);

        if (item.isAuction()) {
            auctionStatusService.validateForPublish(itemId);
        }

        item.publish();
        itemService.flush();
        searchCacheEvictionService.evictItemSearchV2AfterCommit();

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
        searchCacheEvictionService.evictItemSearchV2AfterCommit();

        return new ItemUpdateResponse(item.getId(), item.getUpdatedAt());
    }

    @Transactional
    public ItemStatusUpdateResponse updateItemStatus(Long itemId, Long clientId, ItemStatusUpdateRequest request) {
        Item item = itemService.getItemValidatingAuthor(itemId, clientId);
        item.validateStatusUpdatable();

        Client buyer = resolveBuyer(item, request);

        ItemStatusUpdateResponse response = itemService.updateItemStatus(item, request.tradeStatus(), buyer);
        searchCacheEvictionService.evictItemSearchV2AfterCommit();
        return response;
    }

    private Client resolveBuyer(Item item, ItemStatusUpdateRequest request) {
        if (!isDirectSoldOut(item, request)) {
            return item.getBuyer();
        }

        if (request.buyerId() == null || item.getSeller().getId().equals(request.buyerId())) {
            throw new BusinessException(ErrorCode.INVALID_INPUT);
        }

        return clientService.getClient(request.buyerId());
    }

    private boolean isDirectSoldOut(Item item, ItemStatusUpdateRequest request) {
        return !item.isAuction() && request.tradeStatus() == TradeStatus.SOLD_OUT;
    }

    @Transactional
    public void deleteItem(Long itemId, Long clientId) {
        Item item = itemService.getItemValidatingAuthor(itemId, clientId);

        if (Boolean.TRUE.equals(item.getIsDraft())) {
            auctionStatusService.deleteByItemId(itemId);
            itemImageService.deleteByItemId(itemId);
            itemService.hardDeleteById(itemId);
            return;
        }

        itemService.softDelete(item);
        searchCacheEvictionService.evictItemSearchV2AfterCommit();
    }
}

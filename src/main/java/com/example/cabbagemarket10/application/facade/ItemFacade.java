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
import com.example.cabbagemarket10.domain.item.dto.response.ItemUpdateResponse;
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

    @Transactional
    public ItemUpdateResponse updateItem(Long itemId, Long clientId, ItemUpdateRequest request) {
        // 1. 상품 조회 및 권한 검증 (Item 도메인)
        Item item = itemService.getItemValidatingAuthor(itemId, clientId);

        // 2. 카테고리 검증 및 조회 (Category 도메인)
        Category category = categoryService.getCategory(request.categoryId());

        // 3. 경매 상태 비즈니스 룰 검증 및 수정 (AuctionStatus 도메인)
        auctionStatusService.validateAndUpdateRules(
                itemId,
                item.getInitialPrice(),  // 기존 시작가
                request.initialPrice(),  // 변경 요청 시작가
                request.closeDate()      // 변경 요청 종료일
        );

        // 4. 상품 정보 수정 반영 (더티 체킹)
        item.updateInfo(category, request.title(), request.description(), request.initialPrice());

        return new ItemUpdateResponse(item.getId(), item.getUpdatedAt());
    }
}

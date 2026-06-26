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
        // 1. 회원 및 카테고리 도메인에서 엔티티 조회
        Client seller = clientService.getClient(sellerId);
        Category category = categoryService.getCategory(request.categoryId());

        // 2. Item 도메인에 저장 위임
        Item item = itemService.saveItem(seller, category, request);

        // 3. Auction 도메인에 저장 위임
        auctionStatusService.createAuctionStatus(item, request.initialPrice(), request.closeDate());

        return item.getId();
    }

    @Transactional
    public ItemDraftResponse createItemDraft(Long sellerId, ItemDraftRequest request) {
        // 1. 회원 및 카테고리 도메인에서 엔티티 조회
        Client seller = clientService.getClient(sellerId);
        Category category = null;
        if (request.categoryId() != null) {
            category = categoryService.getCategory(request.categoryId());
        }

        // 2. Item 도메인에 임시저장 위임
        Item item = itemService.saveItemDraft(seller, category, request);

        // 3. Auction 도메인에 선택적 저장 위임
        if (request.closeDate() != null || request.initialPrice() != null) {
            auctionStatusService.createAuctionStatus(
                    item,
                    request.initialPrice() != null ? request.initialPrice() : 0L,
                    request.closeDate()
            );
        }

        return ItemDraftResponse.from(item);
    }
}
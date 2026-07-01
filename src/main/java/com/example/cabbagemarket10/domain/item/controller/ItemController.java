package com.example.cabbagemarket10.domain.item.controller;

import com.example.cabbagemarket10.application.facade.AuctionFacade;
import com.example.cabbagemarket10.application.facade.ItemFacade;
import com.example.cabbagemarket10.domain.item.dto.request.ItemBidRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemDraftRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemStatusUpdateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemUpdateRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemBidResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDetailResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDraftResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemLikeToggleResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemListItemResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemPublishResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemStatusUpdateResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemUpdateResponse;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.itemLike.service.ItemLikeService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.common.PageResponse;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemFacade itemFacade;
    private final ItemService itemService;
    private final ItemLikeService itemLikeService;
    private final ObjectProvider<AuctionFacade> auctionFacadeProvider;

    @PostMapping
    public ResponseEntity<CommonResponse<Long>> createItem(
            @Valid @RequestBody ItemCreateRequest request,
            @AuthenticationPrincipal AuthenticatedClient userDetails) {

        Long sellerId = userDetails.clientId();
        Long itemId = itemFacade.createItem(sellerId, request);

        return CommonResponse.success(HttpStatus.CREATED, itemId).toResponseEntity();
    }

    @PostMapping("/drafts")
    public ResponseEntity<CommonResponse<ItemDraftResponse>> createItemDraft(
            @Valid @RequestBody ItemDraftRequest request,
            @AuthenticationPrincipal AuthenticatedClient userDetails) {

        Long sellerId = userDetails.clientId();
        ItemDraftResponse response = itemFacade.createItemDraft(sellerId, request);

        return CommonResponse.success(HttpStatus.CREATED, response).toResponseEntity();
    }

    @PostMapping("/{itemId}/publish")
    public ResponseEntity<CommonResponse<ItemPublishResponse>> publishItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient userDetails
    ) {
        ItemPublishResponse response = itemFacade.publishItem(itemId, userDetails.clientId());
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }

    @GetMapping
    public ResponseEntity<CommonResponse<PageResponse<ItemListItemResponse>>> getItemList(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String tradeStatus,
            Pageable pageable
    ) {
        Page<ItemListItemResponse> items = itemService.getItemList(categoryId, tradeStatus, pageable);

        // Page 정보를 PageResponse로 변환 (기존 공통 응답 구조 활용)
        PageResponse<ItemListItemResponse> response = PageResponse.from(items);

        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }

    @GetMapping("/{itemId}")
    public ResponseEntity<CommonResponse<ItemDetailResponse>> getItemDetail(@PathVariable Long itemId) {
        ItemDetailResponse response = itemService.getItemDetail(itemId);
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }

    @PostMapping("/{itemId}/likes")
    public ResponseEntity<CommonResponse<ItemLikeToggleResponse>> toggleItemLike(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient userDetails
    ) {
        ItemLikeToggleResponse response = itemLikeService.toggle(itemId, userDetails.clientId());
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }

    @PutMapping("/{itemId}")
    public ResponseEntity<CommonResponse<ItemUpdateResponse>> updateItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient userDetails,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        Long clientId = userDetails.clientId();

        ItemUpdateResponse response = itemFacade.updateItem(itemId, clientId, request);

        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }

    @PatchMapping("/{itemId}/status")
    public ResponseEntity<CommonResponse<ItemStatusUpdateResponse>> updateItemStatus(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient userDetails,
            @Valid @RequestBody ItemStatusUpdateRequest request
    ) {
        ItemStatusUpdateResponse response = itemFacade.updateItemStatus(itemId, userDetails.clientId(), request);
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }

    @DeleteMapping("/{itemId}")
    public ResponseEntity<CommonResponse<Void>> deleteItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient userDetails
    ) {
        itemFacade.deleteItem(itemId, userDetails.clientId());
        return CommonResponse.success(HttpStatus.NO_CONTENT).toResponseEntity();
    }

    @PostMapping("/{itemId}/auction-status/bid")
    public ResponseEntity<CommonResponse<ItemBidResponse>> bid(
            @PathVariable Long itemId,
            @Valid @RequestBody ItemBidRequest request,
            @AuthenticationPrincipal AuthenticatedClient userDetails) {

        AuctionFacade auctionFacade = auctionFacadeProvider.getIfAvailable(() -> {
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        });
        ItemBidResponse response = auctionFacade.bidItem(itemId, userDetails.clientId(), request.getBidPrice());
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }
}

package com.example.cabbagemarket10.domain.item.controller;

import com.example.cabbagemarket10.application.facade.ItemFacade;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemDraftRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemUpdateRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDetailResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDraftResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemListItemResponse;
import com.example.cabbagemarket10.domain.item.dto.response.ItemUpdateResponse;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.common.PageResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemFacade itemFacade;
    private final ItemService itemService;

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

    @PutMapping("/{itemId}")
    public ResponseEntity<CommonResponse<ItemUpdateResponse>> updateItem(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient userDetails,
            @Valid @RequestBody ItemUpdateRequest request
    ) {
        Long clientId = userDetails.clientId();

        // Facade로 흐름 위임
        ItemUpdateResponse response = itemFacade.updateItem(itemId, clientId, request);

        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }
}

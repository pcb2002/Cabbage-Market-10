package com.example.cabbagemarket10.domain.item.controller;

import com.example.cabbagemarket10.application.facade.ItemFacade;
import com.example.cabbagemarket10.domain.item.dto.request.ItemCreateRequest;
import com.example.cabbagemarket10.domain.item.dto.request.ItemDraftRequest;
import com.example.cabbagemarket10.domain.item.dto.response.ItemDraftResponse;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemController {

    private final ItemFacade itemFacade;

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
}

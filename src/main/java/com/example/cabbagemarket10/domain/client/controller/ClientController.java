package com.example.cabbagemarket10.domain.client.controller;

import com.example.cabbagemarket10.domain.client.dto.request.ClientMyInfoUpdateRequest;
import com.example.cabbagemarket10.domain.client.dto.response.ClientMyInfoResponse;
import com.example.cabbagemarket10.domain.client.dto.response.ClientProfileResponse;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.dto.response.MyItemListItemResponse;
import com.example.cabbagemarket10.domain.item.dto.response.MyLikedItemResponse;
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
@RequiredArgsConstructor
@RequestMapping("/api/clients")
public class ClientController {

    private final ClientService clientService;
    private final ItemService itemService;

    @GetMapping("/me")
    public ResponseEntity<CommonResponse<ClientMyInfoResponse>> getMyInfo(
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient
    ) {
        ClientMyInfoResponse response = clientService.getMyInfo(authenticatedClient.clientId());

        return CommonResponse.success(HttpStatus.OK, response)
                .toResponseEntity();
    }

    @GetMapping("/me/items")
    public ResponseEntity<CommonResponse<PageResponse<MyItemListItemResponse>>> getMyItems(
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            @RequestParam(required = false) String tradeStatus,
            Pageable pageable
    ) {
        Page<MyItemListItemResponse> items = itemService.getMyItemList(
                authenticatedClient.clientId(), tradeStatus, pageable);

        return CommonResponse.success(HttpStatus.OK, PageResponse.from(items))
                .toResponseEntity();
    }

    @GetMapping("/me/likes")
    public ResponseEntity<CommonResponse<PageResponse<MyLikedItemResponse>>> getMyLikedItems(
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            Pageable pageable
    ) {
        Page<MyLikedItemResponse> response = clientService.getMyLikedItems(authenticatedClient.clientId(), pageable);

        return CommonResponse.success(HttpStatus.OK, PageResponse.from(response))
                .toResponseEntity();
    }

    @GetMapping("/{clientId}")
    public ResponseEntity<CommonResponse<ClientProfileResponse>> getClientProfile(
            @PathVariable Long clientId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient
    ) {
        Long viewerClientId = authenticatedClient == null ? null : authenticatedClient.clientId();
        ClientProfileResponse response = clientService.getClientProfile(clientId, viewerClientId);

        return CommonResponse.success(HttpStatus.OK, response)
                .toResponseEntity();
    }

    @PatchMapping("/me")
    public ResponseEntity<CommonResponse<ClientMyInfoResponse>> updateMyInfo(
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            @Valid @RequestBody ClientMyInfoUpdateRequest request
    ) {
        ClientMyInfoResponse response = clientService.updateMyInfo(authenticatedClient.clientId(), request);

        return CommonResponse.success(HttpStatus.OK, response)
                .toResponseEntity();
    }
}

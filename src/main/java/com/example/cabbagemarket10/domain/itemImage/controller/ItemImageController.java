package com.example.cabbagemarket10.domain.itemImage.controller;

import com.example.cabbagemarket10.application.facade.ItemImageFacade;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemImageUploadResponse;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemThumbnailUpdateResponse;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/items/{itemId}/images")
@RequiredArgsConstructor
public class ItemImageController {

    private final ItemImageFacade itemImageFacade;

    @PostMapping
    public ResponseEntity<?> uploadItemImages(
            @PathVariable Long itemId,
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedClient userDetails
    ) {
        ItemImageUploadResponse response = itemImageFacade.uploadItemImages(itemId, files, userDetails.clientId());
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }

    @PatchMapping("/{imageId}/thumbnail")
    public ResponseEntity<?> updateThumbnail(
            @PathVariable Long itemId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient
    ) {
        ItemThumbnailUpdateResponse response = itemImageFacade.updateItemThumbnail(itemId, imageId, authenticatedClient.clientId());
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }

    /**
     * 상품 이미지 단독 삭제 API
     */
    @DeleteMapping("/{itemId}/images/{imageId}")
    public ResponseEntity<CommonResponse<Map<String, String>>> deleteItemImage(
            @PathVariable Long itemId,
            @PathVariable Long imageId,
            @RequestAttribute("clientId") Long clientId // security.md 규칙 반영
    ) {
        itemImageFacade.deleteItemImage(itemId, imageId, clientId);

        Map<String, String> response = Map.of("message", "이미지가 성공적으로 삭제되었습니다.");
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }
}
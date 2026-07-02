package com.example.cabbagemarket10.domain.itemImage.controller;

import com.example.cabbagemarket10.application.facade.ItemImageFacade;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemImageUploadResponse;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemThumbnailUpdateResponse;
import com.example.cabbagemarket10.global.common.response.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

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
            @AuthenticationPrincipal AuthenticatedClient userDetails
    ) {
        ItemThumbnailUpdateResponse response =
                itemImageFacade.updateItemThumbnail(itemId, imageId, userDetails.clientId());
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }

    @DeleteMapping("/{imageId}")
    public ResponseEntity<CommonResponse<Void>> deleteItemImage(
            @PathVariable Long itemId,
            @PathVariable Long imageId,
            @AuthenticationPrincipal AuthenticatedClient userDetails
    ) {
        itemImageFacade.deleteItemImage(itemId, imageId, userDetails.clientId());
        return CommonResponse.success(HttpStatus.NO_CONTENT).toResponseEntity();
    }
}

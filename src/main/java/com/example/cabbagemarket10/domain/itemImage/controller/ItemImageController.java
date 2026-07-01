package com.example.cabbagemarket10.domain.itemImage.controller;

import com.example.cabbagemarket10.application.facade.ItemImageFacade;
import com.example.cabbagemarket10.domain.itemImage.dto.response.ItemImageUploadResponse;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class ItemImageController {

    private final ItemImageFacade itemImageFacade;

    @PostMapping("/{itemId}/images")
    public ResponseEntity<?> uploadItemImages(
            @PathVariable Long itemId,
            @RequestPart("files") List<MultipartFile> files,
            @AuthenticationPrincipal AuthenticatedClient userDetails
    ) {
        ItemImageUploadResponse response = itemImageFacade.uploadItemImages(itemId, files, userDetails.clientId());
        return CommonResponse.success(HttpStatus.OK, response).toResponseEntity();
    }
}
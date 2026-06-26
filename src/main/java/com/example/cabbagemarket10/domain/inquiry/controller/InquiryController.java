package com.example.cabbagemarket10.domain.inquiry.controller;

import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryCreateResponse;
import com.example.cabbagemarket10.domain.inquiry.service.InquiryService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/items/{itemId}/inquiries")
public class InquiryController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<CommonResponse<InquiryCreateResponse>> createInquiry(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            @Valid @RequestBody InquiryCreateRequest request
    ) {
        InquiryCreateResponse response = inquiryService.createInquiry(
                itemId,
                authenticatedClient.clientId(),
                request
        );

        return CommonResponse.success(HttpStatus.CREATED, response)
                .toResponseEntity();
    }
}

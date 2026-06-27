package com.example.cabbagemarket10.domain.inquiry.controller;

import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryUpdateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryCreateResponse;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryListResponse;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryUpdateResponse;
import com.example.cabbagemarket10.domain.inquiry.service.InquiryService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api")
public class InquiryController {

    private final InquiryService inquiryService;

    @GetMapping("/items/{itemId}/inquiries")
    public ResponseEntity<CommonResponse<InquiryListResponse>> getInquiries(
            @PathVariable Long itemId,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "20") @Min(1) int size
    ) {
        InquiryListResponse response = inquiryService.getInquiries(itemId, page, size);

        return CommonResponse.success(HttpStatus.OK, response)
                .toResponseEntity();
    }

    @PostMapping("/items/{itemId}/inquiries")
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

    @PutMapping("/inquiries/{inquiryId}")
    public ResponseEntity<CommonResponse<InquiryUpdateResponse>> updateInquiry(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            @Valid @RequestBody InquiryUpdateRequest request
    ) {
        InquiryUpdateResponse response = inquiryService.updateInquiry(
                inquiryId,
                authenticatedClient.clientId(),
                request
        );

        return CommonResponse.success(HttpStatus.OK, response)
                .toResponseEntity();
    }
}

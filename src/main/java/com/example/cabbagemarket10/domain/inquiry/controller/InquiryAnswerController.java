package com.example.cabbagemarket10.domain.inquiry.controller;

import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryAnswerCreateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryAnswerCreateResponse;
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
@RequestMapping("/api/inquiries/{inquiryId}/answer")
public class InquiryAnswerController {

    private final InquiryService inquiryService;

    @PostMapping
    public ResponseEntity<CommonResponse<InquiryAnswerCreateResponse>> createAnswer(
            @PathVariable Long inquiryId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            @Valid @RequestBody InquiryAnswerCreateRequest request
    ) {
        InquiryAnswerCreateResponse response = inquiryService.createAnswer(
                inquiryId,
                authenticatedClient.clientId(),
                request
        );

        return CommonResponse.success(HttpStatus.CREATED, response)
                .toResponseEntity();
    }
}

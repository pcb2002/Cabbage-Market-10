package com.example.cabbagemarket10.domain.review.controller;

import com.example.cabbagemarket10.application.facade.ReviewFacade;
import com.example.cabbagemarket10.domain.review.dto.request.ReviewCreateRequest;
import com.example.cabbagemarket10.domain.review.dto.response.ReceivedReviewListItemResponse;
import com.example.cabbagemarket10.domain.review.dto.response.ReviewCreateResponse;
import com.example.cabbagemarket10.domain.review.dto.response.WrittenReviewListItemResponse;
import com.example.cabbagemarket10.domain.review.service.ReviewService;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ReviewController {

    private final ReviewFacade reviewFacade;
    private final ReviewService reviewService;

    @PostMapping("/items/{itemId}/reviews")
    public ResponseEntity<CommonResponse<ReviewCreateResponse>> createReview(
            @PathVariable Long itemId,
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            @Valid @RequestBody ReviewCreateRequest request
    ) {
        ReviewCreateResponse response = reviewFacade.createReview(
                itemId,
                authenticatedClient.clientId(),
                request
        );

        return CommonResponse.success(HttpStatus.CREATED, response)
                .toResponseEntity();
    }

    @GetMapping("/clients/{clientId}/reviews")
    public ResponseEntity<CommonResponse<PageResponse<ReceivedReviewListItemResponse>>> getReceivedReviews(
            @PathVariable Long clientId,
            Pageable pageable
    ) {
        Page<ReceivedReviewListItemResponse> reviews = reviewFacade.getReceivedReviews(clientId, pageable);

        return CommonResponse.success(HttpStatus.OK, PageResponse.from(reviews))
                .toResponseEntity();
    }

    @GetMapping("/clients/me/reviews/written")
    public ResponseEntity<CommonResponse<PageResponse<WrittenReviewListItemResponse>>> getWrittenReviews(
            @AuthenticationPrincipal AuthenticatedClient authenticatedClient,
            Pageable pageable
    ) {
        Page<WrittenReviewListItemResponse> reviews = reviewService.getWrittenReviews(
                authenticatedClient.clientId(), pageable);

        return CommonResponse.success(HttpStatus.OK, PageResponse.from(reviews))
                .toResponseEntity();
    }
}

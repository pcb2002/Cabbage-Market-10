package com.example.cabbagemarket10.domain.review.dto.response;

import java.time.LocalDateTime;

public record ReceivedReviewListItemResponse(
        Long reviewId,
        Long itemId,
        Long reviewerId,
        String reviewerNickname,
        String reviewerProfileImageUrl,
        Integer rating,
        String content,
        LocalDateTime createdAt
) {}

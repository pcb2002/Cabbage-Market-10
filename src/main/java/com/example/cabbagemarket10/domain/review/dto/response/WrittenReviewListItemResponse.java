package com.example.cabbagemarket10.domain.review.dto.response;

import java.time.LocalDateTime;

public record WrittenReviewListItemResponse(
        Long reviewId,
        Long itemId,
        String itemTitle,
        String itemThumbnailUrl,
        Long revieweeId,
        String revieweeNickname,
        Integer rating,
        String content,
        LocalDateTime createdAt
) {}

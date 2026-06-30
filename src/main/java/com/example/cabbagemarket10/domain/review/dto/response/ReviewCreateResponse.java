package com.example.cabbagemarket10.domain.review.dto.response;

import com.example.cabbagemarket10.domain.review.entity.Review;
import java.time.LocalDateTime;
import lombok.Builder;

@Builder
public record ReviewCreateResponse(
        Long reviewId,
        Long itemId,
        Long reviewerId,
        Long revieweeId,
        Integer rating,
        String content,
        LocalDateTime date
) {

    public static ReviewCreateResponse from(Review review) {
        return ReviewCreateResponse.builder()
                .reviewId(review.getId())
                .itemId(review.getItem().getId())
                .reviewerId(review.getReviewer().getId())
                .revieweeId(review.getReviewee().getId())
                .rating(review.getRating())
                .content(review.getContent())
                .date(review.getCreatedAt())
                .build();
    }
}

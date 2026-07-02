package com.example.cabbagemarket10.domain.review.service;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.review.dto.request.ReviewCreateRequest;
import com.example.cabbagemarket10.domain.review.dto.response.ReceivedReviewListItemResponse;
import com.example.cabbagemarket10.domain.review.dto.response.ReviewCreateResponse;
import com.example.cabbagemarket10.domain.review.dto.response.WrittenReviewListItemResponse;
import com.example.cabbagemarket10.domain.review.entity.Review;
import com.example.cabbagemarket10.domain.review.repository.ReviewRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;

    @Transactional
    public ReviewCreateResponse createReview(Item item, Client reviewer, ReviewCreateRequest request) {
        validateNotReviewed(item.getId(), reviewer.getId());

        Review review = Review.builder()
                .item(item)
                .reviewer(reviewer)
                .reviewee(item.getSeller())
                .rating(request.rating())
                .content(request.content())
                .build();

        try {
            return ReviewCreateResponse.from(reviewRepository.saveAndFlush(review));
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }

    @Transactional(readOnly = true)
    public Page<ReceivedReviewListItemResponse> getReceivedReviews(Long revieweeId, Pageable pageable) {
        return reviewRepository.findReceivedReviews(revieweeId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<WrittenReviewListItemResponse> getWrittenReviews(Long reviewerId, Pageable pageable) {
        return reviewRepository.findWrittenReviews(reviewerId, pageable);
    }

    private void validateNotReviewed(Long itemId, Long reviewerId) {
        if (reviewRepository.existsByItem_IdAndReviewer_Id(itemId, reviewerId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }
    }
}

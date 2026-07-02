package com.example.cabbagemarket10.application.facade;

import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.review.dto.request.ReviewCreateRequest;
import com.example.cabbagemarket10.domain.review.dto.response.ReceivedReviewListItemResponse;
import com.example.cabbagemarket10.domain.review.dto.response.ReviewCreateResponse;
import com.example.cabbagemarket10.domain.review.service.ReviewService;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewFacade {

    private final ItemService itemService;
    private final ClientService clientService;
    private final AuctionStatusService auctionStatusService;
    private final ReviewService reviewService;

    @Transactional
    public ReviewCreateResponse createReview(Long itemId, Long reviewerId, ReviewCreateRequest request) {
        Item item = itemService.getItem(itemId);
        validateReviewableItem(item);
        Client reviewer = clientService.getClient(reviewerId);
        validateBuyer(item, reviewerId);
        return reviewService.createReview(item, reviewer, request);
    }

    @Transactional(readOnly = true)
    public Page<ReceivedReviewListItemResponse> getReceivedReviews(Long clientId, Pageable pageable) {
        clientService.getClient(clientId);
        return reviewService.getReceivedReviews(clientId, pageable);
    }

    private void validateReviewableItem(Item item) {
        if (item.getIsDraft() || item.getTradeStatus() != TradeStatus.SOLD_OUT) {
            throw new BusinessException(ErrorCode.REVIEW_ITEM_NOT_COMPLETED);
        }
    }

    private void validateBuyer(Item item, Long reviewerId) {
        if (item.getSeller().getId().equals(reviewerId)) {
            throw new BusinessException(ErrorCode.SELF_REVIEW_NOT_ALLOWED);
        }

        if (!item.isAuction()) {
            Client buyer = item.getBuyer();
            if (buyer == null || !buyer.getId().equals(reviewerId)) {
                throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
            }
            return;
        }

        Client currentBidder = auctionStatusService.getCurrentBidder(item.getId());
        if (!currentBidder.getId().equals(reviewerId)) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
    }
}

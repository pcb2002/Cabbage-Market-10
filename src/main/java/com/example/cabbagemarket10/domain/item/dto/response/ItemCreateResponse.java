package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import java.time.LocalDateTime;

public record ItemCreateResponse(
        Long itemId,
        Long sellerId,
        Long categoryId,
        TradeType tradeType,
        String title,
        String description,
        Long initialPrice,
        Long currentBid,
        ConditionType conditionType,
        TradeStatus tradeStatus,
        Long viewCount,
        Long likeCount,
        Long inquiryCount,
        Boolean isDraft,
        LocalDateTime closeDate,
        LocalDateTime createdAt
) {
    public static ItemCreateResponse of(Item item, AuctionStatus auctionStatus) {
        return new ItemCreateResponse(
                item.getId(),
                item.getSeller().getId(),
                item.getCategory().getId(),
                item.getTradeType(),
                item.getTitle(),
                item.getDescription(),
                item.getInitialPrice(),
                auctionStatus == null ? null : auctionStatus.getCurrentBid(),
                item.getConditionType(),
                item.getTradeStatus(),
                item.getViewCount(),
                item.getLikeCount(),
                item.getInquiryCount(),
                item.getIsDraft(),
                auctionStatus == null ? null : auctionStatus.getCloseDate(),
                item.getCreatedAt()
        );
    }
}

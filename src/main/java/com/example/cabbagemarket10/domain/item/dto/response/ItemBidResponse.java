package com.example.cabbagemarket10.domain.item.dto.response;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import lombok.Builder;

import java.time.LocalDateTime;

@Builder
public record ItemBidResponse(
        Long itemId,
        Long currentBid,
        LocalDateTime closeDate
) {
    public static ItemBidResponse from(AuctionStatus auctionStatus) {
        return ItemBidResponse.builder()
                .itemId(auctionStatus.getItemId())
                .currentBid(auctionStatus.getCurrentBid())
                .closeDate(auctionStatus.getCloseDate())
                .build();
    }
}

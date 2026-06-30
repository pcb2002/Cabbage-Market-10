package com.example.cabbagemarket10.domain.search.dto.response;

import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;

import java.time.LocalDateTime;

public record SearchItemResponse(
        Long itemId,
        String thumbnailUrl,    // ItemImage(isThumbnail=true).imageUrl, 없으면 null
        String categoryName,    // 크림의 "브랜드" 자리 (카드 상단 소제목)
        String title,
        TradeType tradeType,    // AUCTION/DIRECT - 가격 표시 분기 및 배지용
        Long initialPrice,
        Long currentBid,        // 경매(AUCTION) 상품의 현재 입찰가
        TradeStatus tradeStatus,
        Long likeCount,
        LocalDateTime createdAt
) {
}

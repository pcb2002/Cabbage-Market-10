package com.example.cabbagemarket10.domain.auction.entity;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "auction_status")
public class AuctionStatus {

    @Id
    @Column(name = "item_id")
    private Long itemId;

    @OneToOne
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private Long currentBid;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "current_bidder_id")
    private Client currentBidder;

    @Column(nullable = false)
    private LocalDateTime closeDate;

    @Builder
    public AuctionStatus(Item item, Long currentBid, LocalDateTime closeDate) {
        this.item = item;
        this.itemId = item.getId();
        this.currentBid = currentBid;
        this.closeDate = closeDate;
    }

    // 2. 입찰 검증 로직 추가 및 파라미터 타입 변경
    public void updateBid(Long bidPrice, Client bidder, LocalDateTime currentTime) {
        // 검증 1: 마감 시간 체크
        if (currentTime.isAfter(this.closeDate)) {
            // 프로젝트의 예외 처리 컨벤션(ErrorCode)에 맞게 커스텀 예외로 변경하시는 것을 추천합니다.
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_CLOSED);
        }

        // 검증 2: 입찰가 체크
        if (bidPrice <= this.currentBid) {
            throw new BusinessException(ErrorCode.INVALID_BID_PRICE);
        }

        this.currentBid = bidPrice;
        this.currentBidder = bidder;
    }
}
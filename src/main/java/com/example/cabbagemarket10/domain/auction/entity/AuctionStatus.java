package com.example.cabbagemarket10.domain.auction.entity;

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

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "item_id")
    private Item item;

    @Column(nullable = false)
    private Long currentBid;

    @Column(name = "current_bidder_id")
    private Long currentBidderId;

    @Column(nullable = false)
    private LocalDateTime closeDate;

    @Builder
    public AuctionStatus(Item item, Long currentBid, LocalDateTime closeDate) {
        this.item = item;
        this.currentBid = currentBid;
        this.closeDate = closeDate;
    }

    public void updateBid(Long bidPrice, Long bidderId, LocalDateTime currentTime) {
        if (!currentTime.isBefore(this.closeDate)) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_CLOSED);
        }

        if (bidPrice <= this.currentBid) {
            throw new BusinessException(ErrorCode.INVALID_BID_PRICE);
        }

        this.currentBid = bidPrice;
        this.currentBidderId = bidderId;
    }

    // 경매 종료일 수정 로직
    public void updateCloseDate(LocalDateTime closeDate) {
        this.closeDate = closeDate;
    }

    public void updateCurrentBid(Long currentBid) {
        this.currentBid = currentBid;
    }

    public boolean hasBidder() {
        return this.currentBidderId != null;
    }
}

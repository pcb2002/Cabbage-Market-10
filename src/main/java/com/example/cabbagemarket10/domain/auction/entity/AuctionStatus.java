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

    @OneToOne(fetch = FetchType.LAZY)
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

    public void updateBid(Long bidPrice, Client bidder, LocalDateTime currentTime) {
        if (!currentTime.isBefore(this.closeDate)) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_CLOSED);
        }

        if (bidPrice <= this.currentBid) {
            throw new BusinessException(ErrorCode.INVALID_BID_PRICE);
        }

        this.currentBid = bidPrice;
        this.currentBidder = bidder;
    }
}

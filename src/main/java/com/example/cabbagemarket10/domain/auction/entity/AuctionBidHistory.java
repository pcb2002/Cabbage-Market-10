package com.example.cabbagemarket10.domain.auction.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "auction_bid_history")
@EntityListeners(AuditingEntityListener.class)
public class AuctionBidHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "bidder_id", nullable = false)
    private Long bidderId;

    @Column(name = "previous_bid", nullable = false)
    private Long previousBid;

    @Column(name = "bid_price", nullable = false)
    private Long bidPrice;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public AuctionBidHistory(Long itemId, Long bidderId, Long previousBid, Long bidPrice) {
        this.itemId = itemId;
        this.bidderId = bidderId;
        this.previousBid = previousBid;
        this.bidPrice = bidPrice;
    }
}

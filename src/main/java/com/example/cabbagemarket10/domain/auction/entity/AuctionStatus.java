package com.example.cabbagemarket10.domain.auction.entity;

import com.example.cabbagemarket10.domain.item.entity.Item;
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

    private Long currentBidderId;

    @Column(nullable = false)
    private LocalDateTime closeDate;

    @Builder
    public AuctionStatus(Item item, Long currentBid, LocalDateTime closeDate) {
        this.item = item;
        this.itemId = item.getId();
        this.currentBid = currentBid;
        this.closeDate = closeDate;
    }

    // 입찰 시 정보 업데이트 메서드
    public void updateBid(Long bidPrice, Long bidderId) {
        this.currentBid = bidPrice;
        this.currentBidderId = bidderId;
    }
}
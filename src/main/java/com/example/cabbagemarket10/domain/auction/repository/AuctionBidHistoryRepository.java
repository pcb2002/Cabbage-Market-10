package com.example.cabbagemarket10.domain.auction.repository;

import com.example.cabbagemarket10.domain.auction.entity.AuctionBidHistory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuctionBidHistoryRepository extends JpaRepository<AuctionBidHistory, Long> {
}

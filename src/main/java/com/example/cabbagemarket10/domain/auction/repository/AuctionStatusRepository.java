package com.example.cabbagemarket10.domain.auction.repository;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface AuctionStatusRepository extends JpaRepository<AuctionStatus, Long> {

    @Modifying
    @Query("delete from AuctionStatus auctionStatus where auctionStatus.itemId = :itemId")
    void deleteByItemId(Long itemId);
}

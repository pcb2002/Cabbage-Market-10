package com.example.cabbagemarket10.domain.auction.service;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.repository.AuctionStatusRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuctionStatusService {

    private final AuctionStatusRepository auctionStatusRepository;

    public AuctionStatus createAuctionStatus(Item item, Long initialPrice, LocalDateTime closeDate) {
        AuctionStatus auctionStatus = AuctionStatus.builder()
                .item(item)
                .currentBid(initialPrice)
                .closeDate(closeDate)
                .build();
        return auctionStatusRepository.save(auctionStatus);
    }
}
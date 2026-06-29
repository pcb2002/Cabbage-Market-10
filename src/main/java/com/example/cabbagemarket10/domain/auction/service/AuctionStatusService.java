package com.example.cabbagemarket10.domain.auction.service;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.repository.AuctionStatusRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuctionStatusService {

    private final AuctionStatusRepository auctionStatusRepository;

    public void createAuctionStatus(Item item, Long initialPrice, LocalDateTime closeDate) {
        AuctionStatus auctionStatus = AuctionStatus.builder()
                .item(item)
                .currentBid(initialPrice)
                .closeDate(closeDate)
                .build();
        auctionStatusRepository.save(auctionStatus);
    }

    @Transactional(readOnly = true)
    public void validateForPublish(Long itemId) {
        AuctionStatus auctionStatus = auctionStatusRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_STATUS_NOT_FOUND));

        if (!auctionStatus.getCloseDate().isAfter(LocalDateTime.now())) {
            throw new BusinessException(ErrorCode.AUCTION_ALREADY_CLOSED);
        }
    }

    public void syncDraftAuctionStatus(Item item, Long initialPrice, LocalDateTime closeDate) {
        auctionStatusRepository.findById(item.getId())
                .ifPresentOrElse(
                        auctionStatus -> {
                            auctionStatus.updateCurrentBid(initialPrice);
                            if (closeDate != null) {
                                auctionStatus.updateCloseDate(closeDate);
                            }
                        },
                        () -> {
                            if (closeDate != null) {
                                createAuctionStatus(item, initialPrice, closeDate);
                            }
                        });
    }

    public void deleteByItemId(Long itemId) {
        auctionStatusRepository.deleteByItemId(itemId);
    }
}

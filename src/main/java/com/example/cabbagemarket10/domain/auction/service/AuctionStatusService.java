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

    public AuctionStatus createAuctionStatus(Item item, Long initialPrice, LocalDateTime closeDate) {
        AuctionStatus auctionStatus = AuctionStatus.builder()
                .item(item)
                .currentBid(initialPrice)
                .closeDate(closeDate)
                .build();
        return auctionStatusRepository.save(auctionStatus);
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

    @Transactional
    public void validateAndUpdateRules(Long itemId, Long originalPrice, Long newPrice, LocalDateTime newCloseDate) {
        if (newCloseDate == null) {
            throw new BusinessException(ErrorCode.INVALID_INPUT, "경매 상품은 종료일이 필요합니다.");
        }

        AuctionStatus auctionStatus = auctionStatusRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_STATUS_NOT_FOUND));

        // 1. 입찰자가 이미 존재하는지 확인
        if (auctionStatus.hasBidder()) {
            // 2. 기존과 변경된 값이 있는지 비교
            boolean isPriceChanged = !originalPrice.equals(newPrice);
            boolean isDateChanged = !auctionStatus.getCloseDate().equals(newCloseDate);

            if (isPriceChanged || isDateChanged) {
                throw new BusinessException(ErrorCode.AUCTION_ALREADY_IN_PROGRESS);
            }
        } else {
            // 3. 입찰자가 없을 때만 종료일 수정 반영
            auctionStatus.updateCurrentBid(newPrice);
            auctionStatus.updateCloseDate(newCloseDate);
        }
    }
}

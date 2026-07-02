package com.example.cabbagemarket10.domain.auction.service;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.entity.AuctionBidHistory;
import com.example.cabbagemarket10.domain.auction.repository.AuctionBidHistoryRepository;
import com.example.cabbagemarket10.domain.auction.repository.AuctionStatusRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.dto.response.ItemBidResponse;
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
    private final AuctionBidHistoryRepository auctionBidHistoryRepository;
    private final ClientService clientService;

    public AuctionStatus createAuctionStatus(Item item, Long initialPrice, LocalDateTime closeDate) {
        AuctionStatus auctionStatus = AuctionStatus.builder()
                .item(item)
                .currentBid(initialPrice)
                .closeDate(closeDate)
                .build();
        return auctionStatusRepository.save(auctionStatus);
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

    public Client getCurrentBidder(Long itemId) {
        AuctionStatus auctionStatus = auctionStatusRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED));
        if (auctionStatus.getCurrentBidderId() == null) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }
        return clientService.getClient(auctionStatus.getCurrentBidderId());
    }

    public void deleteByItemId(Long itemId) {
        auctionStatusRepository.deleteByItemId(itemId);
    }

    @Transactional
    public ItemBidResponse bid(Long itemId, Long clientId, Long sellerId, Long bidPrice) {
        if (sellerId.equals(clientId)) {
            throw new BusinessException(ErrorCode.INVALID_BID_REQUEST);
        }

        AuctionStatus auctionStatus = auctionStatusRepository.findById(itemId)
                .orElseThrow(() -> new BusinessException(ErrorCode.AUCTION_STATUS_NOT_FOUND));

        Long previousBid = auctionStatus.getCurrentBid();
        auctionStatus.updateBid(bidPrice, clientId, LocalDateTime.now());
        auctionBidHistoryRepository.save(AuctionBidHistory.builder()
                .itemId(itemId)
                .bidderId(clientId)
                .previousBid(previousBid)
                .bidPrice(bidPrice)
                .build());

        return ItemBidResponse.from(auctionStatus);
    }
}

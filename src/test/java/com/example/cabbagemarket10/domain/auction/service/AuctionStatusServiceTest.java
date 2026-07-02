package com.example.cabbagemarket10.domain.auction.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.auction.entity.AuctionBidHistory;
import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.repository.AuctionBidHistoryRepository;
import com.example.cabbagemarket10.domain.auction.repository.AuctionStatusRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuctionStatusServiceTest {

    @Mock
    private AuctionStatusRepository auctionStatusRepository;

    @Mock
    private AuctionBidHistoryRepository auctionBidHistoryRepository;

    @InjectMocks
    private AuctionStatusService auctionStatusService;

    @DisplayName("입찰가가 현재가보다 높으면 현재 입찰가와 최고 입찰자 ID를 갱신한다")
    @Test
    void bidUpdatesCurrentBidAndCurrentBidderId() {
        AuctionStatus auctionStatus = auctionStatus(10000L, LocalDateTime.now().plusDays(1));
        given(auctionStatusRepository.findById(1L)).willReturn(Optional.of(auctionStatus));

        auctionStatusService.bid(1L, 2L, 3L, 12000L);

        assertThat(auctionStatus.getCurrentBid()).isEqualTo(12000L);
        assertThat(auctionStatus.getCurrentBidderId()).isEqualTo(2L);
        assertThat(auctionStatus.hasBidder()).isTrue();
        ArgumentCaptor<AuctionBidHistory> captor = ArgumentCaptor.forClass(AuctionBidHistory.class);
        verify(auctionBidHistoryRepository).save(captor.capture());
        AuctionBidHistory history = captor.getValue();
        assertThat(history.getItemId()).isEqualTo(1L);
        assertThat(history.getBidderId()).isEqualTo(2L);
        assertThat(history.getPreviousBid()).isEqualTo(10000L);
        assertThat(history.getBidPrice()).isEqualTo(12000L);
    }

    @DisplayName("판매자는 본인 상품에 입찰할 수 없다")
    @Test
    void sellerCannotBidOwnItem() {
        assertThatThrownBy(() -> auctionStatusService.bid(1L, 2L, 2L, 12000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_BID_REQUEST));
    }

    @DisplayName("현재 입찰가 이하로 입찰할 수 없다")
    @Test
    void bidPriceMustBeGreaterThanCurrentBid() {
        AuctionStatus auctionStatus = auctionStatus(10000L, LocalDateTime.now().plusDays(1));
        given(auctionStatusRepository.findById(1L)).willReturn(Optional.of(auctionStatus));

        assertThatThrownBy(() -> auctionStatusService.bid(1L, 2L, 3L, 10000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_BID_PRICE));
    }

    @DisplayName("마감된 경매에는 입찰할 수 없다")
    @Test
    void closedAuctionCannotReceiveBid() {
        AuctionStatus auctionStatus = auctionStatus(10000L, LocalDateTime.now().minusSeconds(1));
        given(auctionStatusRepository.findById(1L)).willReturn(Optional.of(auctionStatus));

        assertThatThrownBy(() -> auctionStatusService.bid(1L, 2L, 3L, 12000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUCTION_ALREADY_CLOSED));
    }

    @DisplayName("경매 상태가 없으면 AUCTION_STATUS_NOT_FOUND 예외가 발생한다")
    @Test
    void bidRequiresAuctionStatus() {
        given(auctionStatusRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> auctionStatusService.bid(1L, 2L, 3L, 12000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUCTION_STATUS_NOT_FOUND));
    }

    private AuctionStatus auctionStatus(Long currentBid, LocalDateTime closeDate) {
        return AuctionStatus.builder()
                .currentBid(currentBid)
                .closeDate(closeDate)
                .build();
    }
}

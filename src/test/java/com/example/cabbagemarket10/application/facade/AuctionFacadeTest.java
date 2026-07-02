package com.example.cabbagemarket10.application.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.dto.response.ItemBidResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.global.config.cache.SearchCacheEvictionService;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuctionFacadeTest {

    @Mock
    private RedissonClient redissonClient;

    @Mock
    private RLock lock;

    @Mock
    private ItemService itemService;

    @Mock
    private AuctionStatusService auctionStatusService;

    @Mock
    private SearchCacheEvictionService searchCacheEvictionService;

    @InjectMocks
    private AuctionFacade auctionFacade;

    @AfterEach
    void clearInterruptedStatus() {
        Thread.interrupted();
    }

    @DisplayName("입찰 락을 획득하면 상품 판매자를 확인하고 입찰을 처리한 뒤 락을 해제한다")
    @Test
    void bidItemUsesRedissonLockAndUnlocks() throws InterruptedException {
        Item item = itemWithSellerId(3L);
        ItemBidResponse expectedResponse = new ItemBidResponse(1L, 12000L, LocalDateTime.now().plusDays(1));
        given(redissonClient.getLock("auction:bid:1")).willReturn(lock);
        given(lock.tryLock(5, 10, TimeUnit.SECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
        given(itemService.getItem(1L)).willReturn(item);
        given(auctionStatusService.bid(1L, 2L, 3L, 12000L)).willReturn(expectedResponse);

        ItemBidResponse response = auctionFacade.bidItem(1L, 2L, 12000L);

        assertThat(response).isEqualTo(expectedResponse);
        verify(auctionStatusService).bid(1L, 2L, 3L, 12000L);
        verify(searchCacheEvictionService).evictItemSearchV2();
        verify(lock).unlock();
    }

    @DisplayName("입찰 락을 획득하지 못하면 입찰을 처리하지 않는다")
    @Test
    void bidItemFailsWhenLockCannotBeAcquired() throws InterruptedException {
        given(redissonClient.getLock("auction:bid:1")).willReturn(lock);
        given(lock.tryLock(5, 10, TimeUnit.SECONDS)).willReturn(false);

        assertThatThrownBy(() -> auctionFacade.bidItem(1L, 2L, 12000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUCTION_BID_LOCK_FAILED));

        verify(itemService, never()).getItem(1L);
        verify(auctionStatusService, never()).bid(1L, 2L, 3L, 12000L);
        verify(searchCacheEvictionService, never()).evictItemSearchV2();
        verify(lock, never()).unlock();
    }

    @DisplayName("입찰 락 대기 중 인터럽트가 발생하면 인터럽트를 복구하고 서버 오류를 반환한다")
    @Test
    void bidItemRestoresInterruptedStatus() throws InterruptedException {
        given(redissonClient.getLock("auction:bid:1")).willReturn(lock);
        given(lock.tryLock(5, 10, TimeUnit.SECONDS)).willThrow(new InterruptedException());

        assertThatThrownBy(() -> auctionFacade.bidItem(1L, 2L, 12000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        verify(auctionStatusService, never()).bid(1L, 2L, 3L, 12000L);
        verify(searchCacheEvictionService, never()).evictItemSearchV2();
        verify(lock, never()).unlock();
    }

    @DisplayName("경매 상품이 아니면 입찰을 처리하지 않고 락을 해제한다")
    @Test
    void directItemCannotReceiveBid() throws InterruptedException {
        assertInvalidBidItem(itemWithSellerId(3L, TradeType.DIRECT, TradeStatus.ON_SALE, false));
    }

    @DisplayName("임시저장 상품이면 입찰을 처리하지 않고 락을 해제한다")
    @Test
    void draftItemCannotReceiveBid() throws InterruptedException {
        assertInvalidBidItem(itemWithSellerId(3L, TradeType.AUCTION, TradeStatus.ON_SALE, true));
    }

    @DisplayName("판매중 상태가 아니면 입찰을 처리하지 않고 락을 해제한다")
    @Test
    void notOnSaleItemCannotReceiveBid() throws InterruptedException {
        assertInvalidBidItem(itemWithSellerId(3L, TradeType.AUCTION, TradeStatus.RESERVED, false));
    }

    private void assertInvalidBidItem(Item item) throws InterruptedException {
        given(redissonClient.getLock("auction:bid:1")).willReturn(lock);
        given(lock.tryLock(5, 10, TimeUnit.SECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
        given(itemService.getItem(1L)).willReturn(item);

        assertThatThrownBy(() -> auctionFacade.bidItem(1L, 2L, 12000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_BID_REQUEST));

        verify(auctionStatusService, never()).bid(1L, 2L, 3L, 12000L);
        verify(searchCacheEvictionService, never()).evictItemSearchV2();
        verify(lock).unlock();
    }

    private Item itemWithSellerId(Long sellerId) {
        return itemWithSellerId(sellerId, TradeType.AUCTION, TradeStatus.ON_SALE, false);
    }

    private Item itemWithSellerId(Long sellerId, TradeType tradeType, TradeStatus tradeStatus, boolean isDraft) {
        Client seller = Client.create(
                "seller@example.com",
                "encodedPassword",
                "seller",
                "seller",
                "010-1234-5678");
        ReflectionTestUtils.setField(seller, "id", sellerId);

        return Item.builder()
                .seller(seller)
                .tradeType(tradeType)
                .title("auction item")
                .description("auction description")
                .initialPrice(10000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(tradeStatus)
                .isDraft(isDraft)
                .build();
    }
}

package com.example.cabbagemarket10.application.facade;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.auction.config.AuctionLockProperties;
import com.example.cabbagemarket10.domain.auction.facade.AuctionFacade;
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
import org.mockito.Spy;
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

    @Spy
    private AuctionLockProperties lockProperties = new AuctionLockProperties();

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

    @DisplayName("입찰은 Redis 락을 획득한 뒤 처리하고 락을 해제한다")
    @Test
    void 입찰은_Redis_락을_획득한_뒤_처리하고_락을_해제한다() throws InterruptedException {
        Item item = itemWithSellerId(3L);
        ItemBidResponse expectedResponse = new ItemBidResponse(1L, 12000L, LocalDateTime.now().plusDays(1));
        given(redissonClient.getLock("auction:bid:1")).willReturn(lock);
        given(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
        given(itemService.getItem(1L)).willReturn(item);
        given(auctionStatusService.bid(1L, 2L, 3L, 12000L)).willReturn(expectedResponse);

        ItemBidResponse response = auctionFacade.bidItem(1L, 2L, 12000L);

        assertThat(response).isEqualTo(expectedResponse);
        verify(auctionStatusService).bid(1L, 2L, 3L, 12000L);
        verify(searchCacheEvictionService).evictItemSearchV2AfterCommit();
        verify(lock).unlock();
    }

    @DisplayName("입찰은 설정된 Redis 락 값을 사용한다")
    @Test
    void 입찰은_설정된_Redis_락_값을_사용한다() throws InterruptedException {
        lockProperties.setKeyPrefix("custom:auction:");
        lockProperties.setWaitTimeMillis(25L);
        lockProperties.setLeaseTimeMillis(50L);
        Item item = itemWithSellerId(3L);
        ItemBidResponse expectedResponse = new ItemBidResponse(1L, 12000L, LocalDateTime.now().plusDays(1));
        given(redissonClient.getLock("custom:auction:1")).willReturn(lock);
        given(lock.tryLock(25L, 50L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
        given(itemService.getItem(1L)).willReturn(item);
        given(auctionStatusService.bid(1L, 2L, 3L, 12000L)).willReturn(expectedResponse);

        ItemBidResponse response = auctionFacade.bidItem(1L, 2L, 12000L);

        assertThat(response).isEqualTo(expectedResponse);
        verify(redissonClient).getLock("custom:auction:1");
        verify(lock).tryLock(25L, 50L, TimeUnit.MILLISECONDS);
        verify(lock).unlock();
    }

    @DisplayName("Redis 락을 획득하지 못하면 입찰에 실패한다")
    @Test
    void Redis_락을_획득하지_못하면_입찰에_실패한다() throws InterruptedException {
        given(redissonClient.getLock("auction:bid:1")).willReturn(lock);
        given(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS)).willReturn(false);

        assertThatThrownBy(() -> auctionFacade.bidItem(1L, 2L, 12000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.AUCTION_BID_LOCK_FAILED));

        verify(itemService, never()).getItem(1L);
        verify(auctionStatusService, never()).bid(1L, 2L, 3L, 12000L);
        verify(searchCacheEvictionService, never()).evictItemSearchV2AfterCommit();
        verify(lock, never()).unlock();
    }

    @DisplayName("입찰 대기 중 인터럽트가 발생하면 인터럽트 상태를 복구한다")
    @Test
    void 입찰_대기_중_인터럽트가_발생하면_인터럽트_상태를_복구한다() throws InterruptedException {
        given(redissonClient.getLock("auction:bid:1")).willReturn(lock);
        given(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS)).willThrow(new InterruptedException());

        assertThatThrownBy(() -> auctionFacade.bidItem(1L, 2L, 12000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INTERNAL_SERVER_ERROR));

        assertThat(Thread.currentThread().isInterrupted()).isTrue();
        verify(auctionStatusService, never()).bid(1L, 2L, 3L, 12000L);
        verify(searchCacheEvictionService, never()).evictItemSearchV2AfterCommit();
        verify(lock, never()).unlock();
    }

    @DisplayName("직거래 상품은 입찰할 수 없다")
    @Test
    void 직거래_상품은_입찰할_수_없다() throws InterruptedException {
        assertInvalidBidItem(itemWithSellerId(3L, TradeType.DIRECT, TradeStatus.ON_SALE, false));
    }

    @DisplayName("임시저장 상품은 입찰할 수 없다")
    @Test
    void 임시저장_상품은_입찰할_수_없다() throws InterruptedException {
        assertInvalidBidItem(itemWithSellerId(3L, TradeType.AUCTION, TradeStatus.ON_SALE, true));
    }

    @DisplayName("판매중이 아닌 상품은 입찰할 수 없다")
    @Test
    void 판매중이_아닌_상품은_입찰할_수_없다() throws InterruptedException {
        assertInvalidBidItem(itemWithSellerId(3L, TradeType.AUCTION, TradeStatus.RESERVED, false));
    }

    private void assertInvalidBidItem(Item item) throws InterruptedException {
        given(redissonClient.getLock("auction:bid:1")).willReturn(lock);
        given(lock.tryLock(5000L, 10000L, TimeUnit.MILLISECONDS)).willReturn(true);
        given(lock.isHeldByCurrentThread()).willReturn(true);
        given(itemService.getItem(1L)).willReturn(item);

        assertThatThrownBy(() -> auctionFacade.bidItem(1L, 2L, 12000L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INVALID_BID_REQUEST));

        verify(auctionStatusService, never()).bid(1L, 2L, 3L, 12000L);
        verify(searchCacheEvictionService, never()).evictItemSearchV2AfterCommit();
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

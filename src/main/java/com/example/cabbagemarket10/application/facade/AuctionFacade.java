package com.example.cabbagemarket10.application.facade;

import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.item.dto.response.ItemBidResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.global.config.cache.SearchCacheEvictionService;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.util.concurrent.TimeUnit;
import lombok.RequiredArgsConstructor;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "app.auction.redis-lock.enabled", havingValue = "true", matchIfMissing = true)
public class AuctionFacade {

    private static final String BID_LOCK_KEY_PREFIX = "auction:bid:";

    private final RedissonClient redissonClient;
    private final ItemService itemService;
    private final AuctionStatusService auctionStatusService;
    private final SearchCacheEvictionService searchCacheEvictionService;

    public ItemBidResponse bidItem(Long itemId, Long clientId, Long bidPrice) {
        RLock lock = redissonClient.getLock(BID_LOCK_KEY_PREFIX + itemId);
        boolean locked = false;
        try {
            locked = lock.tryLock(5, 10, TimeUnit.SECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.AUCTION_BID_LOCK_FAILED);
            }

            Item item = itemService.getItem(itemId);
            item.validateBiddable();
            ItemBidResponse response = auctionStatusService.bid(itemId, clientId, item.getSeller().getId(), bidPrice);
            searchCacheEvictionService.evictItemSearchV2();
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BusinessException(ErrorCode.INTERNAL_SERVER_ERROR);
        } finally {
            if (locked && lock.isHeldByCurrentThread()) {
                lock.unlock();
            }
        }
    }
}

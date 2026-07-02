package com.example.cabbagemarket10.domain.auction.facade;

import com.example.cabbagemarket10.domain.auction.config.AuctionLockProperties;
import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.item.dto.response.ItemBidResponse;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.service.ItemService;
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

    private final RedissonClient redissonClient;
    private final AuctionLockProperties lockProperties;
    private final ItemService itemService;
    private final AuctionStatusService auctionStatusService;

    public ItemBidResponse bidItem(Long itemId, Long clientId, Long bidPrice) {
        RLock lock = redissonClient.getLock(lockProperties.getKeyPrefix() + itemId);
        boolean locked = false;
        try {
            locked = lock.tryLock(
                    lockProperties.getWaitTimeMillis(),
                    lockProperties.getLeaseTimeMillis(),
                    TimeUnit.MILLISECONDS);
            if (!locked) {
                throw new BusinessException(ErrorCode.AUCTION_BID_LOCK_FAILED);
            }

            Item item = itemService.getItem(itemId);
            item.validateBiddable();
            return auctionStatusService.bid(itemId, clientId, item.getSeller().getId(), bidPrice);
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

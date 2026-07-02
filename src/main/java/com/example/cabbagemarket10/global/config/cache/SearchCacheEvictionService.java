package com.example.cabbagemarket10.global.config.cache;

import lombok.RequiredArgsConstructor;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

@Service
@RequiredArgsConstructor
public class SearchCacheEvictionService {

    private final CacheManager cacheManager;

    public void evictItemSearchV2AfterCommit() {
        if (!TransactionSynchronizationManager.isSynchronizationActive()
                || !TransactionSynchronizationManager.isActualTransactionActive()) {
            evictItemSearchV2Now();
            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                evictItemSearchV2Now();
            }
        });
    }

    void evictItemSearchV2Now() {
        Cache cache = cacheManager.getCache(ItemSearchCacheConfig.ITEM_SEARCH_V2_CACHE);
        if (cache != null) {
            cache.clear();
        }
    }
}

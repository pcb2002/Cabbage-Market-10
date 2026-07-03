package com.example.cabbagemarket10.global.config.cache;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

class SearchCacheEvictionServiceTest {

    private final CacheManager cacheManager = Mockito.mock(CacheManager.class);
    private final Cache cache = Mockito.mock(Cache.class);
    private final SearchCacheEvictionService searchCacheEvictionService =
            new SearchCacheEvictionService(cacheManager);

    @DisplayName("트랜잭션이 없으면 즉시 검색 캐시를 비운다")
    @Test
    void 트랜잭션이_없으면_즉시_검색_캐시를_비운다() {
        Mockito.when(cacheManager.getCache(ItemSearchCacheConfig.ITEM_SEARCH_V2_CACHE)).thenReturn(cache);

        searchCacheEvictionService.evictItemSearchV2AfterCommit();

        verify(cache).clear();
    }

    @DisplayName("트랜잭션이 있으면 커밋 이후 검색 캐시를 비운다")
    @Test
    void 트랜잭션이_있으면_커밋_이후_검색_캐시를_비운다() {
        Mockito.when(cacheManager.getCache(ItemSearchCacheConfig.ITEM_SEARCH_V2_CACHE)).thenReturn(cache);
        TransactionSynchronizationManager.initSynchronization();
        TransactionSynchronizationManager.setActualTransactionActive(true);
        try {
            searchCacheEvictionService.evictItemSearchV2AfterCommit();

            verify(cache, never()).clear();

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager.getSynchronizations()) {
                synchronization.afterCommit();
            }

            verify(cache).clear();
        } finally {
            TransactionSynchronizationManager.setActualTransactionActive(false);
            TransactionSynchronizationManager.clearSynchronization();
        }
    }
}

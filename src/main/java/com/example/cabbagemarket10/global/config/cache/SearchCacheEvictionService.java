package com.example.cabbagemarket10.global.config.cache;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.stereotype.Service;

@Service
public class SearchCacheEvictionService {

    @CacheEvict(cacheNames = ItemSearchCacheConfig.ITEM_SEARCH_V2_CACHE, allEntries = true)
    public void evictItemSearchV2() {
    }
}

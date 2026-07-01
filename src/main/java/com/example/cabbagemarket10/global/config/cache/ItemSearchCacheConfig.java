package com.example.cabbagemarket10.global.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableCaching
@EnableConfigurationProperties(ItemSearchCacheProperties.class)
public class ItemSearchCacheConfig {

    public static final String ITEM_SEARCH_V2_CACHE = "itemSearchV2";

    @Bean
    public CacheManager cacheManager(ItemSearchCacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of(ITEM_SEARCH_V2_CACHE));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.ttl())
                .maximumSize(properties.maximumSize()));
        return cacheManager;
    }
}

package com.example.cabbagemarket10.global.config.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import java.util.List;
import java.util.Set;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.JdkSerializationRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

@Configuration
@EnableCaching
@EnableConfigurationProperties(ItemSearchCacheProperties.class)
public class ItemSearchCacheConfig {

    public static final String ITEM_SEARCH_V2_CACHE = "itemSearchV2";

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "app.cache.item-search-v2",
            name = "type",
            havingValue = "local"
    )
    public CacheManager localCacheManager(ItemSearchCacheProperties properties) {
        CaffeineCacheManager cacheManager = new CaffeineCacheManager();
        cacheManager.setCacheNames(List.of(ITEM_SEARCH_V2_CACHE));
        cacheManager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(properties.ttl())
                .maximumSize(properties.maximumSize()));
        return cacheManager;
    }

    @Bean
    @Primary
    @ConditionalOnProperty(
            prefix = "app.cache.item-search-v2",
            name = "type",
            havingValue = "redis",
            matchIfMissing = true
    )
    public CacheManager redisCacheManager(
            RedisConnectionFactory redisConnectionFactory,
            ItemSearchCacheProperties properties
    ) {
        RedisCacheConfiguration cacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(properties.ttl())
                .computePrefixWith(cacheName -> properties.keyPrefix() + cacheName + "::")
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(new JdkSerializationRedisSerializer()));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(cacheConfiguration)
                .initialCacheNames(Set.of(ITEM_SEARCH_V2_CACHE))
                .build();
    }
}

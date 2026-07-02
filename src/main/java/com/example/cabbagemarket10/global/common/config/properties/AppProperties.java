package com.example.cabbagemarket10.global.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cache cache,
        Auction auction
) {
    public record Cache(ItemSearchV2 itemSearchV2) {}
    public record ItemSearchV2(String type) {}

    public record Auction(RedisLock redisLock) {}
    public record RedisLock(boolean enabled) {}
}
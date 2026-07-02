package com.example.cabbagemarket10.global.common.config.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        Cache cache,
        Auction auction,
        Cors cors
) {
    public record Cache(ItemSearchV2 itemSearchV2) {}
    public record ItemSearchV2(String type) {}

    public record Auction(RedisLock redisLock) {}
    public record RedisLock(boolean enabled) {}
    public record Cors(List<String> allowedOrigins) {}
}
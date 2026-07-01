package com.example.cabbagemarket10.global.config.cache;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.cache.item-search-v2")
public record ItemSearchCacheProperties(
        Duration ttl,
        long maximumSize
) {

    public ItemSearchCacheProperties {
        ttl = ttl == null ? Duration.ofMinutes(5) : ttl;
        maximumSize = maximumSize > 0 ? maximumSize : 1000L;
    }
}

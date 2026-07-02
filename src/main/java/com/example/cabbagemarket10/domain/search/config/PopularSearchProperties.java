package com.example.cabbagemarket10.domain.search.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.search.popular")
public class PopularSearchProperties {

    private int limit = 10;
    private Duration dedupTtl = Duration.ofMinutes(1);
    private Duration dailyKeyTtl = Duration.ofDays(8);
    private String dailyKeyPrefix = "popular:search:";
    private String dedupKeyPrefix = "popular:dedup:";

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit > 0 ? limit : 10;
    }

    public Duration getDedupTtl() {
        return dedupTtl;
    }

    public void setDedupTtl(Duration dedupTtl) {
        this.dedupTtl = dedupTtl == null ? Duration.ofMinutes(1) : dedupTtl;
    }

    public Duration getDailyKeyTtl() {
        return dailyKeyTtl;
    }

    public void setDailyKeyTtl(Duration dailyKeyTtl) {
        this.dailyKeyTtl = dailyKeyTtl == null ? Duration.ofDays(8) : dailyKeyTtl;
    }

    public String getDailyKeyPrefix() {
        return dailyKeyPrefix;
    }

    public void setDailyKeyPrefix(String dailyKeyPrefix) {
        this.dailyKeyPrefix = hasText(dailyKeyPrefix) ? dailyKeyPrefix : "popular:search:";
    }

    public String getDedupKeyPrefix() {
        return dedupKeyPrefix;
    }

    public void setDedupKeyPrefix(String dedupKeyPrefix) {
        this.dedupKeyPrefix = hasText(dedupKeyPrefix) ? dedupKeyPrefix : "popular:dedup:";
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

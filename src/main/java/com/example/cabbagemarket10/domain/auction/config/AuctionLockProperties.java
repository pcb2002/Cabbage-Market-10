package com.example.cabbagemarket10.domain.auction.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auction.redis-lock")
public class AuctionLockProperties {

    private String keyPrefix = "auction:bid:";
    private long waitTimeMillis = 5_000L;
    private long leaseTimeMillis = 10_000L;

    public String getKeyPrefix() {
        return keyPrefix;
    }

    public void setKeyPrefix(String keyPrefix) {
        this.keyPrefix = keyPrefix;
    }

    public long getWaitTimeMillis() {
        return waitTimeMillis;
    }

    public void setWaitTimeMillis(long waitTimeMillis) {
        this.waitTimeMillis = waitTimeMillis;
    }

    public long getLeaseTimeMillis() {
        return leaseTimeMillis;
    }

    public void setLeaseTimeMillis(long leaseTimeMillis) {
        this.leaseTimeMillis = leaseTimeMillis;
    }
}

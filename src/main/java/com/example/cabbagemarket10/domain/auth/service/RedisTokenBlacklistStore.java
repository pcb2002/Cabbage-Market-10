package com.example.cabbagemarket10.domain.auth.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
@Profile("!test")
@RequiredArgsConstructor
public class RedisTokenBlacklistStore implements TokenBlacklistStore {

    private static final String KEY_PREFIX = "auth:blacklist:";
    private static final String BLACKLIST_VALUE = "1";

    private final StringRedisTemplate stringRedisTemplate;
    private final Clock clock;

    @Override
    public void blacklist(String jti, Instant expiresAt) {
        Duration ttl = Duration.between(Instant.now(clock), expiresAt);
        if (ttl.isNegative() || ttl.isZero()) {
            return;
        }
        stringRedisTemplate.opsForValue().set(KEY_PREFIX + jti, BLACKLIST_VALUE, ttl);
    }

    @Override
    public boolean isBlacklisted(String jti) {
        Boolean exists = stringRedisTemplate.hasKey(KEY_PREFIX + jti);
        return Boolean.TRUE.equals(exists);
    }
}

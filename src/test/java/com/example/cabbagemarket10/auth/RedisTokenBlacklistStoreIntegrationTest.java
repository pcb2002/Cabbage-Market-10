package com.example.cabbagemarket10.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cabbagemarket10.domain.auth.service.RedisTokenBlacklistStore;
import com.example.cabbagemarket10.domain.auth.service.TokenBlacklistStore;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("redis-test")
class RedisTokenBlacklistStoreIntegrationTest {

    @Autowired
    private TokenBlacklistStore tokenBlacklistStore;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @DisplayName("로컬 Redis에 블랙리스트를 TTL과 함께 저장하고 만료 후 자동 삭제한다")
    @Test
    void 로컬_Redis에_블랙리스트를_TTL과_함께_저장하고_만료_후_자동_삭제한다() throws Exception {
        assertThat(tokenBlacklistStore).isInstanceOf(RedisTokenBlacklistStore.class);

        String jti = "ttl-test-jti";
        tokenBlacklistStore.blacklist(jti, Instant.now().plusSeconds(1));

        assertThat(tokenBlacklistStore.isBlacklisted(jti)).isTrue();
        assertThat(stringRedisTemplate.hasKey("auth:blacklist:" + jti)).isTrue();

        Thread.sleep(1500L);

        assertThat(tokenBlacklistStore.isBlacklisted(jti)).isFalse();
        assertThat(stringRedisTemplate.hasKey("auth:blacklist:" + jti)).isFalse();
    }
}

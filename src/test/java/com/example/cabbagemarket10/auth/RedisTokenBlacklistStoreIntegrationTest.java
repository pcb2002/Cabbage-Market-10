package com.example.cabbagemarket10.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.example.cabbagemarket10.domain.auth.service.RedisTokenBlacklistStore;
import com.example.cabbagemarket10.domain.auth.service.TokenBlacklistStore;
import java.io.IOException;
import java.net.ServerSocket;
import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import redis.embedded.RedisServer;

@SpringBootTest
@ActiveProfiles("redis-test")
class RedisTokenBlacklistStoreIntegrationTest {

    private static final int REDIS_PORT = findAvailablePort();
    private static RedisServer redisServer;

    @Autowired
    private TokenBlacklistStore tokenBlacklistStore;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) throws IOException {
        startEmbeddedRedis();
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> REDIS_PORT);
        registry.add("spring.data.redis.password", () -> "");
    }

    @AfterAll
    static void stopEmbeddedRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @DisplayName("embedded Redis에 블랙리스트를 TTL과 함께 저장하고 만료 후 자동 삭제한다")
    @Test
    void embedded_Redis에_블랙리스트를_TTL과_함께_저장하고_만료_후_자동_삭제한다() throws Exception {
        assertThat(tokenBlacklistStore).isInstanceOf(RedisTokenBlacklistStore.class);

        String jti = "ttl-test-jti";
        tokenBlacklistStore.blacklist(jti, Instant.now().plusSeconds(1));

        assertThat(tokenBlacklistStore.isBlacklisted(jti)).isTrue();
        assertThat(stringRedisTemplate.hasKey("auth:blacklist:" + jti)).isTrue();

        Thread.sleep(1500L);

        assertThat(tokenBlacklistStore.isBlacklisted(jti)).isFalse();
        assertThat(stringRedisTemplate.hasKey("auth:blacklist:" + jti)).isFalse();
    }

    @DisplayName("embedded Redis에 정지 회원 마커를 TTL과 함께 저장하고 만료 후 자동 삭제한다")
    @Test
    void embedded_Redis에_정지_회원_마커를_TTL과_함께_저장하고_만료_후_자동_삭제한다() throws Exception {
        assertThat(tokenBlacklistStore).isInstanceOf(RedisTokenBlacklistStore.class);

        Long clientId = 1L;
        stringRedisTemplate.opsForValue()
                .set("auth:suspended-client:" + clientId, "1", Duration.ofSeconds(1));

        assertThat(tokenBlacklistStore.isSuspendedClientMarked(clientId)).isTrue();
        assertThat(stringRedisTemplate.hasKey("auth:suspended-client:" + clientId)).isTrue();

        Thread.sleep(1500L);

        assertThat(tokenBlacklistStore.isSuspendedClientMarked(clientId)).isFalse();
        assertThat(stringRedisTemplate.hasKey("auth:suspended-client:" + clientId)).isFalse();
    }

    private static void startEmbeddedRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            return;
        }
        redisServer = RedisServer.newRedisServer()
                .bind("127.0.0.1")
                .port(REDIS_PORT)
                .setting("save \"\"")
                .setting("appendonly no")
                .build();
        redisServer.start();
    }

    private static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("embedded Redis port를 할당할 수 없습니다.", exception);
        }
    }
}

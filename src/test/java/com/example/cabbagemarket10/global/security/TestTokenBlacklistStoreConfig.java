package com.example.cabbagemarket10.global.security;

import com.example.cabbagemarket10.domain.auth.service.TokenBlacklistStore;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

@Profile("test")
@Configuration
public class TestTokenBlacklistStoreConfig {

    @Bean
    @Primary
    TokenBlacklistStore tokenBlacklistStore() {
        return new InMemoryTokenBlacklistStore();
    }

    static class InMemoryTokenBlacklistStore implements TokenBlacklistStore {

        private final Map<String, Instant> storage = new ConcurrentHashMap<>();

        @Override
        public void blacklist(String jti, Instant expiresAt) {
            storage.put(jti, expiresAt);
        }

        @Override
        public boolean isBlacklisted(String jti) {
            return storage.containsKey(jti);
        }
    }
}

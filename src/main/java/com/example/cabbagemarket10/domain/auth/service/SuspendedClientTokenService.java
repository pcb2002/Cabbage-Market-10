package com.example.cabbagemarket10.domain.auth.service;

import com.example.cabbagemarket10.global.security.jwt.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SuspendedClientTokenService {

    private final TokenBlacklistStore tokenBlacklistStore;
    private final JwtProperties jwtProperties;
    private final Clock clock;

    public void markSuspendedClient(Long clientId) {
        Instant expiresAt = Instant.now(clock).plusSeconds(jwtProperties.accessTokenExpireSeconds());
        tokenBlacklistStore.markSuspendedClient(clientId, expiresAt);
    }

    public void clearSuspendedClient(Long clientId) {
        tokenBlacklistStore.removeSuspendedClient(clientId);
    }
}

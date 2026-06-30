package com.example.cabbagemarket10.domain.auth.service;

import java.time.Instant;

public interface TokenBlacklistStore {

    void blacklist(String jti, Instant expiresAt);

    boolean isBlacklisted(String jti);

    void markSuspendedClient(Long clientId, Instant expiresAt);

    boolean isSuspendedClientMarked(Long clientId);

    void removeSuspendedClient(Long clientId);
}

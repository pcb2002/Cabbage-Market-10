package com.example.cabbagemarket10.domain.auth.service;

import java.time.Instant;

public interface TokenBlacklistStore {

    void blacklist(String jti, Instant expiresAt);

    boolean isBlacklisted(String jti);

    boolean isSuspendedClientMarked(Long clientId);
}

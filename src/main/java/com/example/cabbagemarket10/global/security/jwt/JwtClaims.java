package com.example.cabbagemarket10.global.security.jwt;

import java.time.Instant;

public record JwtClaims(
        Long clientId,
        String email,
        String role,
        String jti,
        Instant issuedAt,
        Instant expiresAt) {}

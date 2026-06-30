package com.example.cabbagemarket10.domain.auth.service;

import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.jwt.JwtClaims;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BlackListService {

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistStore tokenBlacklistStore;

    public void blacklistAccessToken(String accessToken) {
        JwtClaims claims = jwtTokenProvider.validateAccessToken(accessToken);
        validateAndBlacklist(claims.jti(), claims.expiresAt());
    }

    public void validateAndBlacklist(String jti, Instant expiresAt) {
        if (tokenBlacklistStore.isBlacklisted(jti)) {
            throw new BusinessException(ErrorCode.BLACKLISTED_TOKEN);
        }
        tokenBlacklistStore.blacklist(jti, expiresAt);
    }
}

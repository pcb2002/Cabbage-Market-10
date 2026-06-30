package com.example.cabbagemarket10.auth;

import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.auth.service.SuspendedClientTokenService;
import com.example.cabbagemarket10.domain.auth.service.TokenBlacklistStore;
import com.example.cabbagemarket10.global.security.jwt.JwtProperties;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class SuspendedClientTokenServiceTest {

    private static final Instant BASE_TIME = Instant.parse("2026-06-25T00:00:00Z");

    @Mock
    private TokenBlacklistStore tokenBlacklistStore;

    private SuspendedClientTokenService suspendedClientTokenService;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties("test-secret", 3600L, 1209600L);
        Clock clock = Clock.fixed(BASE_TIME, ZoneOffset.UTC);
        suspendedClientTokenService = new SuspendedClientTokenService(tokenBlacklistStore, jwtProperties, clock);
    }

    @DisplayName("정지 회원 PK 마커를 Access Token 만료 시간까지 저장한다")
    @Test
    void 정지_회원_PK_마커를_Access_Token_만료_시간까지_저장한다() {
        Long clientId = 1L;

        suspendedClientTokenService.markSuspendedClient(clientId);

        verify(tokenBlacklistStore).markSuspendedClient(clientId, BASE_TIME.plusSeconds(3600L));
    }

    @DisplayName("정지 해제 시 회원 PK 마커를 삭제한다")
    @Test
    void 정지_해제_시_회원_PK_마커를_삭제한다() {
        Long clientId = 1L;

        suspendedClientTokenService.clearSuspendedClient(clientId);

        verify(tokenBlacklistStore).removeSuspendedClient(clientId);
    }
}

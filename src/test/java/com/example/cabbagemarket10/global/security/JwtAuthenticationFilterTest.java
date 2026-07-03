package com.example.cabbagemarket10.global.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cabbagemarket10.domain.auth.service.TokenBlacklistStore;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.global.common.response.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import com.example.cabbagemarket10.global.security.jwt.JwtClaims;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import({JwtAuthenticationFilterTest.TestController.class, JwtAuthenticationFilterTest.ClockTestConfig.class})
class JwtAuthenticationFilterTest {

    private static final Instant BASE_TIME = Instant.parse("2026-06-25T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private TokenBlacklistStore tokenBlacklistStore;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from review");
        jdbcTemplate.update("delete from client");
        clock.setInstant(BASE_TIME);
    }

    @DisplayName("Authorization Bearer Access Token 헤더로 인증 객체가 생성된다")
    @Test
    void Authorization_Bearer_Access_Token_헤더로_인증_객체가_생성된다() throws Exception {
        Client client = saveClient("client@example.com");
        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(client.getId()))
                .andExpect(jsonPath("$.data.email").value("client@example.com"));
    }

    @DisplayName("인증 필터는 사용자 DB 조회 없이 JWT Claim으로 인증한다")
    @Test
    void 인증_필터는_사용자_DB_조회_없이_JWT_Claim으로_인증한다() throws Exception {
        Client client = saveClient("deleted@example.com");
        String accessToken = jwtTokenProvider.createAccessToken(client);
        jdbcTemplate.update("delete from client");

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.clientId").value(client.getId()))
                .andExpect(jsonPath("$.data.email").value("deleted@example.com"));
    }

    @DisplayName("잘못된 토큰은 인증 실패 공통 오류 응답으로 처리된다")
    @Test
    void 잘못된_토큰은_인증_실패_공통_오류_응답으로_처리된다() throws Exception {
        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_INVALID"));
    }

    @DisplayName("Bearer 형식이 아닌 Authorization 헤더는 인증하지 않는다")
    @Test
    void Bearer_형식이_아닌_Authorization_헤더는_인증하지_않는다() throws Exception {
        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Basic invalid-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("만료된 토큰은 인증 실패 공통 오류 응답으로 처리된다")
    @Test
    void 만료된_토큰은_인증_실패_공통_오류_응답으로_처리된다() throws Exception {
        Client client = saveClient("expired@example.com");
        String accessToken = jwtTokenProvider.createAccessToken(client);
        clock.setInstant(BASE_TIME.plusSeconds(3601));

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("ACCESS_TOKEN_EXPIRED"));
    }

    @DisplayName("블랙리스트 토큰은 인증 실패 공통 오류 응답으로 처리된다")
    @Test
    void 블랙리스트_토큰은_인증_실패_공통_오류_응답으로_처리된다() throws Exception {
        Client client = saveClient("blacklisted@example.com");
        String accessToken = jwtTokenProvider.createAccessToken(client);
        JwtClaims claims = jwtTokenProvider.validateAccessToken(accessToken);
        tokenBlacklistStore.blacklist(claims.jti(), claims.expiresAt());

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("BLACKLISTED_TOKEN"));
    }

    @DisplayName("정지 회원 마커가 있으면 기존 Access Token을 거부하고 토큰을 블랙리스트 처리한다")
    @Test
    void 정지_회원_마커가_있으면_기존_Access_Token을_거부하고_토큰을_블랙리스트_처리한다() throws Exception {
        Client client = saveClient("suspended-token@example.com");
        String accessToken = jwtTokenProvider.createAccessToken(client);
        JwtClaims claims = jwtTokenProvider.validateAccessToken(accessToken);
        putSuspendedClientMarker(client.getId(), claims.expiresAt());

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("SUSPENDED_ACCOUNT"));

        assertThat(tokenBlacklistStore.isBlacklisted(claims.jti())).isTrue();
        assertThat(tokenBlacklistStore.isSuspendedClientMarked(client.getId())).isTrue();

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("BLACKLISTED_TOKEN"));
    }

    @DisplayName("정지 회원 마커는 유지되어 같은 회원의 다른 Access Token도 차단한다")
    @Test
    void 정지_회원_마커는_유지되어_같은_회원의_다른_Access_Token도_차단한다() throws Exception {
        Client client = saveClient("multi-token-suspended@example.com");
        String firstAccessToken = jwtTokenProvider.createAccessToken(client);
        String secondAccessToken = jwtTokenProvider.createAccessToken(client);
        JwtClaims firstClaims = jwtTokenProvider.validateAccessToken(firstAccessToken);
        JwtClaims secondClaims = jwtTokenProvider.validateAccessToken(secondAccessToken);
        putSuspendedClientMarker(client.getId(), firstClaims.expiresAt());

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + firstAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUSPENDED_ACCOUNT"));

        mockMvc.perform(get("/api/test/protected")
                        .header("Authorization", "Bearer " + secondAccessToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SUSPENDED_ACCOUNT"));

        assertThat(tokenBlacklistStore.isBlacklisted(firstClaims.jti())).isTrue();
        assertThat(tokenBlacklistStore.isBlacklisted(secondClaims.jti())).isTrue();
        assertThat(tokenBlacklistStore.isSuspendedClientMarked(client.getId())).isTrue();
    }

    @DisplayName("인증이 필요한 API는 토큰 없이 접근할 수 없다")
    @Test
    void 인증이_필요한_API는_토큰_없이_접근할_수_없다() throws Exception {
        mockMvc.perform(get("/api/test/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private Client saveClient(String email) {
        return clientRepository.save(Client.create(
                email,
                passwordEncoder.encode("password123!"),
                "배추판매자",
                "홍길동",
                "010-1234-5678"));
    }

    private void putSuspendedClientMarker(Long clientId, Instant expiresAt) {
        TestTokenBlacklistStoreConfig.InMemoryTokenBlacklistStore store =
                (TestTokenBlacklistStoreConfig.InMemoryTokenBlacklistStore) tokenBlacklistStore;
        store.putSuspendedClientMarker(clientId, expiresAt);
    }

    @RestController
    @RequestMapping("/api/test")
    static class TestController {

        @GetMapping("/protected")
        ResponseEntity<CommonResponse<AuthenticatedClient>> protectedApi(
                @AuthenticationPrincipal AuthenticatedClient client) {
            return CommonResponse.success(HttpStatus.OK, client).toResponseEntity();
        }
    }

    @TestConfiguration
    static class ClockTestConfig {

        @Bean
        @Primary
        MutableClock testClock() {
            return new MutableClock(BASE_TIME, ZoneOffset.UTC);
        }
    }

    static class MutableClock extends Clock {

        private Instant instant;
        private final ZoneId zone;

        MutableClock(Instant instant, ZoneId zone) {
            this.instant = instant;
            this.zone = zone;
        }

        void setInstant(Instant instant) {
            this.instant = instant;
        }

        @Override
        public ZoneId getZone() {
            return zone;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new MutableClock(instant, zone);
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}

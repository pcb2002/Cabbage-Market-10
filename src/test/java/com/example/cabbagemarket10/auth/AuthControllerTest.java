package com.example.cabbagemarket10.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cabbagemarket10.domain.auth.service.AuthCookieManager;
import com.example.cabbagemarket10.domain.client.entity.AccountStatus;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import jakarta.servlet.http.Cookie;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.hamcrest.Matchers;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@SpringBootTest
@AutoConfigureMockMvc
@Import({AuthControllerTest.ClockTestConfig.class, AuthControllerTest.TestController.class})
class AuthControllerTest {

    private static final Instant BASE_TIME = Instant.parse("2026-06-30T00:00:00Z");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private MutableClock clock;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from auction_status");
        jdbcTemplate.update("delete from item");
        jdbcTemplate.update("delete from category");
        jdbcTemplate.update("delete from client");
        clock.setInstant(BASE_TIME);
    }

    @DisplayName("회원가입 성공 시 비밀번호와 전화번호를 제외한 회원 정보를 반환한다")
    @Test
    void 회원가입_성공_시_비밀번호를_제외한_회원_정보를_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.com",
                                  "password": "password123!",
                                  "nickname": "cabbage",
                                  "name": "client",
                                  "phone": "010-1234-5678"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.clientId").exists())
                .andExpect(jsonPath("$.data.email").value("client@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("cabbage"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist());

        Client client = clientRepository.findByEmail("client@example.com").orElseThrow();

        assertThat(client.getProfileImageUrl()).isEqualTo(Client.defaultProfileImageUrl());
    }

    @DisplayName("회원가입 시 전화번호가 없으면 400을 반환한다")
    @Test
    void 회원가입_시_전화번호가_없으면_400을_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "default-image@example.com",
                                  "password": "password123!",
                                  "nickname": "defaultimage",
                                  "name": "client"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("이미 가입된 이메일이면 회원가입에 실패한다")
    @Test
    void 이미_가입된_이메일이면_회원가입에_실패한다() throws Exception {
        clientRepository.save(Client.create(
                "client@example.com",
                passwordEncoder.encode("password123!"),
                "cabbage",
                "client",
                "010-1234-5678"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.com",
                                  "password": "password123!",
                                  "nickname": "other",
                                  "name": "other",
                                  "phone": "010-9999-8888"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATED_EMAIL"));
    }

    @DisplayName("탈퇴 회원 이메일로는 재가입할 수 없다")
    @Test
    void 탈퇴한_회원의_이메일로는_재가입할_수_없다() throws Exception {
        Client withdrawn = clientRepository.save(Client.create(
                "withdrawn@example.com",
                passwordEncoder.encode("password123!"),
                "withdrawn",
                "client",
                "010-1234-5678"));
        clientRepository.delete(withdrawn);

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "withdrawn@example.com",
                                  "password": "password123!",
                                  "nickname": "newcabbage",
                                  "name": "newclient",
                                  "phone": "010-9999-8888"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATED_EMAIL"));
    }

    @DisplayName("회원가입 비밀번호는 PasswordEncoder로 암호화되어 저장된다")
    @Test
    void 회원가입_비밀번호는_PasswordEncoder로_암호화되어_저장된다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "encoded@example.com",
                                  "password": "password123!",
                                  "nickname": "encoded",
                                  "name": "client",
                                  "phone": "010-1234-5678"
                                }
                                """))
                .andExpect(status().isCreated());

        Client client = clientRepository.findByEmail("encoded@example.com").orElseThrow();

        assertThat(client.getPassword()).isNotEqualTo("password123!");
        assertThat(passwordEncoder.matches("password123!", client.getPassword())).isTrue();
    }

    @DisplayName("로그인 성공 시 Access Token 헤더와 Refresh Token 쿠키를 반환한다")
    @Test
    void 로그인_성공_시_Access_Token_헤더와_Refresh_Token_쿠키를_반환한다() throws Exception {
        clientRepository.save(Client.create(
                "client@example.com",
                passwordEncoder.encode("password123!"),
                "cabbage",
                "client",
                "010-1234-5678"));

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, Matchers.startsWith("Bearer ")))
                .andExpect(jsonPath("$.data").value(Matchers.nullValue()))
                .andReturn();

        assertThat(result.getResponse().getCookie(AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME)).isNotNull();
        assertThat(requireCookie(result, "XSRF-TOKEN")).isNotNull();
    }

    @DisplayName("Refresh Token 재발급 성공 시 새 Access Token과 새 Refresh Token을 반환한다")
    @Test
    void Refresh_Token_재발급_성공_시_새_Access_Token과_새_Refresh_Token을_반환한다() throws Exception {
        saveClient("refresh@example.com");

        MvcResult loginResult = login("refresh@example.com", "password123!");
        Cookie refreshCookie = loginResult.getResponse().getCookie(AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME);
        Cookie xsrfCookie = requireCookie(loginResult, "XSRF-TOKEN");

        MvcResult refreshResult = mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie, xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, Matchers.startsWith("Bearer ")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("refresh_token=")))
                .andReturn();

        Cookie newRefreshCookie = refreshResult.getResponse().getCookie(AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME);
        assertThat(newRefreshCookie).isNotNull();
        assertThat(newRefreshCookie.getValue()).isNotEqualTo(refreshCookie.getValue());
    }

    @DisplayName("Refresh Token 쿠키가 없으면 재발급에 실패한다")
    @Test
    void Refresh_Token_쿠키가_없으면_재발급에_실패한다() throws Exception {
        saveClient("missing-refresh@example.com");
        MvcResult loginResult = login("missing-refresh@example.com", "password123!");
        Cookie xsrfCookie = requireCookie(loginResult, "XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @DisplayName("유효하지 않은 Refresh Token이면 재발급에 실패한다")
    @Test
    void 유효하지_않은_Refresh_Token이면_재발급에_실패한다() throws Exception {
        saveClient("invalid-refresh@example.com");
        MvcResult loginResult = login("invalid-refresh@example.com", "password123!");
        Cookie xsrfCookie = requireCookie(loginResult, "XSRF-TOKEN");
        Cookie invalidRefreshCookie = new Cookie(AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME, "invalid-refresh-token");

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(invalidRefreshCookie, xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("INVALID_REFRESH_TOKEN"));
    }

    @DisplayName("만료된 Refresh Token이면 재발급에 실패한다")
    @Test
    void 만료된_Refresh_Token이면_재발급에_실패한다() throws Exception {
        saveClient("expired-refresh@example.com");
        MvcResult loginResult = login("expired-refresh@example.com", "password123!");
        Cookie refreshCookie = loginResult.getResponse().getCookie(AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME);
        Cookie xsrfCookie = requireCookie(loginResult, "XSRF-TOKEN");

        clock.setInstant(BASE_TIME.plusSeconds(1209601));

        mockMvc.perform(post("/api/auth/refresh")
                        .cookie(refreshCookie, xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("REFRESH_TOKEN_EXPIRED"));
    }

    @DisplayName("로그아웃 성공 시 Access Token은 블랙리스트 처리되고 Refresh Token 쿠키는 만료된다")
    @Test
    void 로그아웃_성공_시_Access_Token은_블랙리스트_처리되고_Refresh_Token_쿠키는_만료된다() throws Exception {
        saveClient("logout@example.com");

        MvcResult loginResult = login("logout@example.com", "password123!");
        String authorizationHeader = loginResult.getResponse().getHeader(HttpHeaders.AUTHORIZATION);
        Cookie refreshCookie = loginResult.getResponse().getCookie(AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME);
        Cookie xsrfCookie = requireCookie(loginResult, "XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/logout")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader)
                        .cookie(refreshCookie, xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, Matchers.containsString("Max-Age=0")));

        mockMvc.perform(get("/api/test/protected")
                        .header(HttpHeaders.AUTHORIZATION, authorizationHeader))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BLACKLISTED_TOKEN"));
    }

    @DisplayName("비인증 로그아웃 요청은 401을 반환한다")
    @Test
    void 비인증_로그아웃_요청은_401을_반환한다() throws Exception {
        saveClient("unauthorized-logout@example.com");
        MvcResult loginResult = login("unauthorized-logout@example.com", "password123!");
        Cookie refreshCookie = loginResult.getResponse().getCookie(AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME);
        Cookie xsrfCookie = requireCookie(loginResult, "XSRF-TOKEN");

        mockMvc.perform(post("/api/auth/logout")
                        .cookie(refreshCookie, xsrfCookie)
                        .header("X-XSRF-TOKEN", xsrfCookie.getValue()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("로그인 실패 시 공통 오류 응답을 반환한다")
    @Test
    void 로그인_실패_시_공통_오류_응답을_반환한다() throws Exception {
        clientRepository.save(Client.create(
                "client@example.com",
                passwordEncoder.encode("password123!"),
                "cabbage",
                "client",
                "010-1234-5678"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.com",
                                  "password": "wrong123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @DisplayName("탈퇴 회원은 로그인할 수 없다")
    @Test
    void 탈퇴_회원은_로그인할_수_없다() throws Exception {
        Client withdrawn = clientRepository.save(Client.create(
                "withdrawn-login@example.com",
                passwordEncoder.encode("password123!"),
                "withdrawn",
                "client",
                "010-1234-5678"));
        clientRepository.delete(withdrawn);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "withdrawn-login@example.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"));
    }

    @DisplayName("정지 회원은 로그인할 수 없다")
    @Test
    void 정지_회원은_로그인할_수_없다() throws Exception {
        Client client = Client.create(
                "suspended@example.com",
                passwordEncoder.encode("password123!"),
                "suspended",
                "client",
                "010-1234-5678");
        ReflectionTestUtils.setField(client, "status", AccountStatus.SUSPENDED);
        clientRepository.save(client);

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "suspended@example.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("SUSPENDED_ACCOUNT"));
    }

    private Client saveClient(String email) {
        return clientRepository.save(Client.create(
                email,
                passwordEncoder.encode("password123!"),
                "cabbage",
                "client",
                "010-1234-5678"));
    }

    private MvcResult login(String email, String password) throws Exception {
        return mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "%s",
                                  "password": "%s"
                                }
                                """.formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn();
    }

    private Cookie requireCookie(MvcResult result, String name) {
        Cookie cookie = result.getResponse().getCookie(name);
        assertThat(cookie).isNotNull();
        return cookie;
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

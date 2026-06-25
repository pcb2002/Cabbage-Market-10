package com.example.cabbagemarket10.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cabbagemarket10.domain.client.entity.AccountStatus;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.util.ReflectionTestUtils;

@SpringBootTest
@AutoConfigureMockMvc
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from client");
    }

    @DisplayName("회원가입 성공 시 비밀번호를 제외한 회원 정보를 반환한다")
    @Test
    void 회원가입_성공_시_비밀번호를_제외한_회원_정보를_반환한다() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.com",
                                  "password": "password123!",
                                  "nickname": "배추판매자",
                                  "name": "홍길동",
                                  "phone": "010-1234-5678"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.clientId").exists())
                .andExpect(jsonPath("$.data.email").value("client@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("배추판매자"))
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist());
    }

    @DisplayName("이미 가입된 이메일이면 회원가입에 실패한다")
    @Test
    void 이미_가입된_이메일이면_회원가입에_실패한다() throws Exception {
        clientRepository.save(Client.create(
                "client@example.com",
                passwordEncoder.encode("password123!"),
                "배추판매자",
                "홍길동",
                "010-1234-5678"));

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.com",
                                  "password": "password123!",
                                  "nickname": "배추판매자2",
                                  "name": "김길동"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("DUPLICATED_EMAIL"));
    }

    @DisplayName("탈퇴한 회원의 이메일로는 재가입할 수 없다")
    @Test
    void 탈퇴한_회원의_이메일로는_재가입할_수_없다() throws Exception {
        Client withdrawn = clientRepository.save(Client.create(
                "withdrawn@example.com",
                passwordEncoder.encode("password123!"),
                "탈퇴배추",
                "홍길동",
                "010-1234-5678"));
        clientRepository.delete(withdrawn); // @SQLDelete → is_deleted = true (soft delete)

        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "withdrawn@example.com",
                                  "password": "password123!",
                                  "nickname": "재가입배추",
                                  "name": "김길동",
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
                                  "nickname": "암호배추",
                                  "name": "홍길동"
                                }
                                """))
                .andExpect(status().isCreated());

        Client client = clientRepository.findByEmail("encoded@example.com").orElseThrow();

        assertThat(client.getPassword()).isNotEqualTo("password123!");
        assertThat(passwordEncoder.matches("password123!", client.getPassword())).isTrue();
    }

    @DisplayName("로그인 성공 시 Access Token을 응답 헤더로 반환한다")
    @Test
    void 로그인_성공_시_Access_Token을_응답_헤더로_반환한다() throws Exception {
        clientRepository.save(Client.create(
                "client@example.com",
                passwordEncoder.encode("password123!"),
                "배추판매자",
                "홍길동",
                "010-1234-5678"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "client@example.com",
                                  "password": "password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(header().string(HttpHeaders.AUTHORIZATION, org.hamcrest.Matchers.startsWith("Bearer ")))
                .andExpect(jsonPath("$.data").value(org.hamcrest.Matchers.nullValue()));
    }

    @DisplayName("로그인 실패 시 공통 오류 응답을 반환한다")
    @Test
    void 로그인_실패_시_공통_오류_응답을_반환한다() throws Exception {
        clientRepository.save(Client.create(
                "client@example.com",
                passwordEncoder.encode("password123!"),
                "배추판매자",
                "홍길동",
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
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @DisplayName("탈퇴 회원은 로그인할 수 없다")
    @Test
    void 탈퇴_회원은_로그인할_수_없다() throws Exception {
        Client withdrawn = clientRepository.save(Client.create(
                "withdrawn-login@example.com",
                passwordEncoder.encode("password123!"),
                "탈퇴배추",
                "홍길동",
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
                .andExpect(jsonPath("$.code").value("LOGIN_FAILED"))
                .andExpect(jsonPath("$.message").value("이메일 또는 비밀번호가 올바르지 않습니다."));
    }

    @DisplayName("정지 회원은 로그인할 수 없다")
    @Test
    void 정지_회원은_로그인할_수_없다() throws Exception {
        Client client = Client.create(
                "suspended@example.com",
                passwordEncoder.encode("password123!"),
                "정지배추",
                "홍길동",
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
                .andExpect(jsonPath("$.code").value("SUSPENDED_ACCOUNT"))
                .andExpect(jsonPath("$.message").value("정지된 회원은 로그인할 수 없습니다."));
    }
}

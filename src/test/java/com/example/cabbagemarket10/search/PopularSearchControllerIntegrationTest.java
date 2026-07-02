package com.example.cabbagemarket10.domain.search.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.List;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import redis.embedded.RedisServer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("redis-test")
class PopularSearchControllerIntegrationTest {

    private static final int REDIS_PORT = findAvailablePort();
    private static RedisServer redisServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) throws IOException {
        startEmbeddedRedis();
        registry.add("spring.data.redis.host", () -> "127.0.0.1");
        registry.add("spring.data.redis.port", () -> REDIS_PORT);
        registry.add("spring.data.redis.password", () -> "");
    }

    @AfterAll
    static void stopEmbeddedRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            redisServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
        jdbcTemplate.update("delete from review");
        jdbcTemplate.update("delete from inquiry_log");
        jdbcTemplate.update("delete from auction_status");
        jdbcTemplate.update("delete from item");
        jdbcTemplate.update("delete from category");
        jdbcTemplate.update("delete from client");
    }

    @DisplayName("비회원 인기 검색어 조회는 세션 중복 검색을 한 번만 집계한다")
    @Test
    void 비회원_인기_검색어_조회는_세션_중복_검색을_한_번만_집계한다() throws Exception {
        준비된_상품을_저장한다();

        MockHttpSession firstSession = new MockHttpSession();
        MockHttpSession secondSession = new MockHttpSession();

        mockMvc.perform(get("/api/v2/items/search")
                        .session(firstSession)
                        .param("keyword", "감자"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v2/items/search")
                        .session(firstSession)
                        .param("keyword", "감자"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v2/items/search")
                        .session(secondSession)
                        .param("keyword", "감자"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/search/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.keywords.length()").value(1))
                .andExpect(jsonPath("$.data.keywords[0].rank").value(1))
                .andExpect(jsonPath("$.data.keywords[0].keyword").value("감자"))
                .andExpect(jsonPath("$.data.keywords[0].score").value(2));
    }

    @DisplayName("회원 인기 검색어 조회는 같은 회원의 반복 검색을 한 번만 집계한다")
    @Test
    void 회원_인기_검색어_조회는_같은_회원의_반복_검색을_한_번만_집계한다() throws Exception {
        준비된_상품을_저장한다();

        Client firstClient = clientRepository.save(Client.create(
                "popular1@example.com",
                passwordEncoder.encode("password123!"),
                "popular1",
                "popular1",
                "010-3234-5678"));
        Client secondClient = clientRepository.save(Client.create(
                "popular2@example.com",
                passwordEncoder.encode("password123!"),
                "popular2",
                "popular2",
                "010-4234-5678"));

        mockMvc.perform(get("/api/v1/items/search")
                        .with(authentication(authenticationOf(firstClient)))
                        .param("keyword", "양파"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/items/search")
                        .with(authentication(authenticationOf(firstClient)))
                        .param("keyword", "양파"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/api/v1/items/search")
                        .with(authentication(authenticationOf(secondClient)))
                        .param("keyword", "양파"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/search/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.keywords[0].keyword").value("양파"))
                .andExpect(jsonPath("$.data.keywords[0].score").value(2));
    }

    @DisplayName("인기 검색어가 없으면 빈 배열을 반환한다")
    @Test
    void 인기_검색어가_없으면_빈_배열을_반환한다() throws Exception {
        mockMvc.perform(get("/api/search/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.keywords.length()").value(0));
    }

    @DisplayName("로그인 없이 likedOnly 검색에 실패하면 인기 검색어에 집계되지 않는다")
    @Test
    void 비로그인_likedOnly_검색_실패는_인기_검색어에_집계되지_않는다() throws Exception {
        준비된_상품을_저장한다();

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "당근")
                        .param("likedOnly", "true"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/search/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.keywords.length()").value(0));
    }

    @DisplayName("잘못된 가격 범위로 검색에 실패하면 인기 검색어에 집계되지 않는다")
    @Test
    void 잘못된_가격_범위_검색_실패는_인기_검색어에_집계되지_않는다() throws Exception {
        준비된_상품을_저장한다();

        mockMvc.perform(get("/api/v2/items/search")
                        .param("keyword", "배추")
                        .param("minPrice", "2000")
                        .param("maxPrice", "1000"))
                .andExpect(status().isBadRequest());

        mockMvc.perform(get("/api/search/popular"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.keywords.length()").value(0));
    }

    private void 준비된_상품을_저장한다() {
        Client seller = clientRepository.save(Client.create(
                "popular-seller@example.com",
                passwordEncoder.encode("password123!"),
                "popularSeller",
                "popularSeller",
                "010-1234-9999"));
        Category category = categoryRepository.save(Category.builder()
                .name("popular-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("감자 판매")
                .description("양파도 같이 있습니다.")
                .initialPrice(1000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.NEW)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());
    }

    private UsernamePasswordAuthenticationToken authenticationOf(Client client) {
        AuthenticatedClient principal = new AuthenticatedClient(client.getId(), client.getEmail());
        return UsernamePasswordAuthenticationToken.authenticated(
                principal,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private static void startEmbeddedRedis() throws IOException {
        if (redisServer != null && redisServer.isActive()) {
            return;
        }
        redisServer = RedisServer.newRedisServer()
                .bind("127.0.0.1")
                .port(REDIS_PORT)
                .setting("save \"\"")
                .setting("appendonly no")
                .build();
        redisServer.start();
    }

    private static int findAvailablePort() {
        try (ServerSocket socket = new ServerSocket(0)) {
            return socket.getLocalPort();
        } catch (IOException exception) {
            throw new IllegalStateException("embedded Redis port를 할당할 수 없습니다.", exception);
        }
    }
}

package com.example.cabbagemarket10.domain.search.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
import com.example.cabbagemarket10.domain.itemImage.repository.ItemImageRepository;
import java.io.IOException;
import java.net.ServerSocket;
import java.util.Set;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import redis.embedded.RedisServer;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("redis-test")
class RedisItemSearchCacheIntegrationTest {

    private static final int REDIS_PORT = findAvailablePort();
    private static RedisServer redisServer;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemImageRepository itemImageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private CacheManager cacheManager;

    @Autowired
    private StringRedisTemplate stringRedisTemplate;

    private Category vegetableCategory;

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
        deleteIfExists("chat_message");
        deleteIfExists("chat_room");
        deleteIfExists("inquiry_log");
        deleteIfExists("item_image");
        deleteIfExists("follow");
        deleteIfExists("review");
        deleteIfExists("auction_status");
        deleteIfExists("item");
        deleteIfExists("category");
        deleteIfExists("client");

        Client seller = clientRepository.save(Client.create(
                "redis-search-seller@example.com",
                passwordEncoder.encode("password123!"),
                "redisSearchSeller",
                "redisSearchSeller",
                "010-9999-9999"));

        vegetableCategory = categoryRepository.save(Category.builder()
                .name("채소")
                .sortOrder(1)
                .isActive(true)
                .build());

        saveDirectItem(seller, "레디스 캐시 배추", "레디스 저장 확인", 10_000L);

        Cache itemSearchV2Cache = cacheManager.getCache("itemSearchV2");
        if (itemSearchV2Cache != null) {
            itemSearchV2Cache.clear();
        }
        stringRedisTemplate.getConnectionFactory().getConnection().serverCommands().flushDb();
    }

    @DisplayName("상품 검색 v2는 조회 결과를 Redis 캐시에 저장한다")
    @Test
    void 상품_검색_v2는_조회_결과를_Redis_캐시에_저장한다() throws Exception {
        mockMvc.perform(get("/api/v2/items/search")
                        .param("keyword", "배추")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1));

        Set<String> keys = stringRedisTemplate.keys("item-search:v2:itemSearchV2::*");

        assertThat(keys).isNotNull();
        assertThat(keys).hasSize(1);
    }

    @DisplayName("상품 검색 v2는 Redis 캐시된 결과를 재사용한다")
    @Test
    void 상품_검색_v2는_Redis_캐시된_결과를_재사용한다() throws Exception {
        Item cachedItem = itemRepository.findAll().get(0);

        mockMvc.perform(get("/api/v2/items/search")
                        .param("keyword", "배추")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].itemId").value(cachedItem.getId()));

        itemRepository.delete(cachedItem);
        itemRepository.flush();

        mockMvc.perform(get("/api/v2/items/search")
                        .param("keyword", "배추")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].itemId").value(cachedItem.getId()));
    }

    private Item saveDirectItem(Client seller, String title, String description, long price) {
        Item item = itemRepository.saveAndFlush(Item.builder()
                .seller(seller)
                .category(vegetableCategory)
                .title(title)
                .description(description)
                .initialPrice(price)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        itemImageRepository.save(ItemImage.builder()
                .item(item)
                .imageUrl("https://cdn.example.com/items/%d.jpg".formatted(item.getId()))
                .sortOrder(0)
                .isThumbnail(true)
                .build());

        return item;
    }

    private void deleteIfExists(String tableName) {
        try {
            jdbcTemplate.execute("DELETE FROM " + tableName);
        } catch (org.springframework.dao.DataAccessException ignored) {
        }
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

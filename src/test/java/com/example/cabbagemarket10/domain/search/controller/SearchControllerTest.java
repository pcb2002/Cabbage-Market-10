package com.example.cabbagemarket10.domain.search.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cabbagemarket10.domain.auction.entity.AuctionStatus;
import com.example.cabbagemarket10.domain.auction.repository.AuctionStatusRepository;
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
import java.time.LocalDateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private AuctionStatusRepository auctionStatusRepository;

    @Autowired
    private ItemImageRepository itemImageRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Client seller;
    private Category vegetableCategory;
    private Category deviceCategory;

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

        seller = clientRepository.save(Client.create(
                "search-seller@example.com",
                passwordEncoder.encode("password123!"),
                "searchSeller",
                "searchSeller",
                "010-1234-5678"));
        vegetableCategory = categoryRepository.save(Category.builder()
                .name("채소")
                .sortOrder(1)
                .isActive(true)
                .build());
        deviceCategory = categoryRepository.save(Category.builder()
                .name("전자기기")
                .sortOrder(2)
                .isActive(true)
                .build());
    }

    @DisplayName("상품 검색은 비로그인 사용자도 접근할 수 있다")
    @Test
    void searchItemsAllowsAnonymousAccess() throws Exception {
        saveDirectItem("비로그인 검색 배추", "인증 없이 조회 가능", vegetableCategory, 10_000L);

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "배추")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].title").value("비로그인 검색 배추"));
    }

    @DisplayName("상품 검색 keyword가 100자를 초과하면 400을 반환한다")
    @Test
    void searchItemsWithKeywordOverMaxLengthReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "가".repeat(101)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("상품 검색은 제목 또는 설명에 LIKE 조건을 적용한다")
    @Test
    void searchItemsMatchesKeywordAgainstTitleOrDescription() throws Exception {
        saveDirectItem("제목 매칭 배추", "일반 설명", vegetableCategory, 10_000L);
        saveDirectItem("일반 제목", "설명에 배추 포함", vegetableCategory, 12_000L);
        saveDirectItem("감자 상품", "다른 설명", vegetableCategory, 8_000L);

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "배추")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(2));
    }

    @DisplayName("상품 검색은 categoryId와 tradeStatus 조건을 함께 적용한다")
    @Test
    void searchItemsAppliesCategoryAndTradeStatusFiltersTogether() throws Exception {
        Item expected = saveDirectItem("필터 대상 배추", "판매중 채소", vegetableCategory, 10_000L);
        saveDirectItem("다른 카테고리 배추", "판매중 전자기기", deviceCategory, 11_000L);
        saveItem("예약 배추", "예약 상태", vegetableCategory, 12_000L,
                TradeType.DIRECT, ConditionType.USED, TradeStatus.RESERVED, false);

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "배추")
                        .param("categoryId", String.valueOf(vegetableCategory.getId()))
                        .param("tradeStatus", "ON_SALE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].itemId").value(expected.getId()))
                .andExpect(jsonPath("$.data.content[0].tradeStatus").value("ON_SALE"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @DisplayName("상품 검색은 삭제 상품과 임시저장 상품을 제외한다")
    @Test
    void searchItemsExcludesDeletedAndDraftItems() throws Exception {
        Item visible = saveDirectItem("노출 배추", "검색 대상", vegetableCategory, 10_000L);
        Item deleted = saveDirectItem("삭제 배추", "검색 제외", vegetableCategory, 11_000L);
        saveItem("임시저장 배추", "검색 제외", vegetableCategory, 12_000L,
                TradeType.DIRECT, ConditionType.USED, TradeStatus.ON_SALE, true);

        itemRepository.delete(deleted);
        itemRepository.flush();

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "배추")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].itemId").value(visible.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @DisplayName("상품 검색은 최신순과 낮은 가격순, 높은 가격순, 현재 입찰가순 정렬을 지원한다")
    @Test
    void searchItemsSupportsAllowedSorts() throws Exception {
        Item cheap = saveDirectItem("정렬 저가 배추", "정렬", vegetableCategory, 1_000L);
        Item expensive = saveDirectItem("정렬 고가 배추", "정렬", vegetableCategory, 9_000L);
        Item lowBid = saveAuctionItem("정렬 낮은 입찰 배추", "정렬", vegetableCategory, 5_000L, 7_000L);
        Item highBid = saveAuctionItem("정렬 높은 입찰 배추", "정렬", vegetableCategory, 6_000L, 15_000L);

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "정렬")
                        .param("sort", "createdAt,desc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].itemId").value(highBid.getId()));

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "정렬")
                        .param("sort", "initialPrice,asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].itemId").value(cheap.getId()));

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "정렬")
                        .param("sort", "initialPrice,desc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].itemId").value(expensive.getId()));

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "정렬")
                        .param("sort", "currentBid,desc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(2))
                .andExpect(jsonPath("$.data.content[0].itemId").value(highBid.getId()))
                .andExpect(jsonPath("$.data.content[1].itemId").value(lowBid.getId()));
    }

    @DisplayName("상품 검색 비지원 sort는 기본 최신순으로 처리하고 일반거래 상품을 제외하지 않는다")
    @Test
    void searchItemsWithUnsupportedSortFallsBackToDefaultWithoutFilteringDirectItems() throws Exception {
        Item direct = saveDirectItem("비지원 정렬 직거래 배추", "검색 대상", vegetableCategory, 10_000L);

        mockMvc.perform(get("/api/v1/items/search")
                        .param("keyword", "비지원")
                        .param("sort", "currentBid,asc")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].itemId").value(direct.getId()))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    private Item saveDirectItem(String title, String description, Category category, Long initialPrice) {
        return saveItem(title, description, category, initialPrice,
                TradeType.DIRECT, ConditionType.USED, TradeStatus.ON_SALE, false);
    }

    private Item saveAuctionItem(
            String title,
            String description,
            Category category,
            Long initialPrice,
            Long currentBid
    ) {
        Item item = saveItem(title, description, category, initialPrice,
                TradeType.AUCTION, ConditionType.USED, TradeStatus.ON_SALE, false);
        auctionStatusRepository.save(AuctionStatus.builder()
                .item(item)
                .currentBid(currentBid)
                .closeDate(LocalDateTime.of(2026, 8, 1, 10, 0, 0))
                .build());
        return item;
    }

    private Item saveItem(
            String title,
            String description,
            Category category,
            Long initialPrice,
            TradeType tradeType,
            ConditionType conditionType,
            TradeStatus tradeStatus,
            boolean isDraft
    ) {
        Item item = itemRepository.saveAndFlush(Item.builder()
                .seller(seller)
                .category(category)
                .title(title)
                .description(description)
                .initialPrice(initialPrice)
                .tradeType(tradeType)
                .conditionType(conditionType)
                .tradeStatus(tradeStatus)
                .isDraft(isDraft)
                .build());

        if (!isDraft) {
            itemImageRepository.save(ItemImage.builder()
                    .item(item)
                    .imageUrl("https://cdn.example.com/items/%d.jpg".formatted(item.getId()))
                    .sortOrder(0)
                    .isThumbnail(true)
                    .build());
        }
        return item;
    }

    private void deleteIfExists(String tableName) {
        try {
            jdbcTemplate.update("delete from " + tableName);
        } catch (DataAccessException ignored) {
        }
    }
}

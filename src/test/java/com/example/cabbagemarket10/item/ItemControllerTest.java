package com.example.cabbagemarket10.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import com.example.cabbagemarket10.global.security.jwt.AuthenticatedClient;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
class ItemControllerTest {

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
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from review");
        jdbcTemplate.update("delete from item_like");
        jdbcTemplate.update("delete from inquiry_log");
        jdbcTemplate.update("delete from auction_status");
        jdbcTemplate.update("delete from item");
        jdbcTemplate.update("delete from category");
        jdbcTemplate.update("delete from client");
    }

    @AfterEach
    void tearDown() {
        jdbcTemplate.update("delete from item_like");
    }

    @DisplayName("인증 회원이 상품을 등록하면 Item과 AuctionStatus가 함께 저장된다")
    @Test
    void 인증_회원이_상품을_등록하면_Item과_AuctionStatus가_함께_저장된다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "seller@example.com",
                passwordEncoder.encode("password123!"),
                "판매자",
                "김판매",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("채소")
                .sortOrder(1)
                .isActive(true)
                .build());
        LocalDateTime closeDate = LocalDateTime.of(2026, 6, 30, 23, 59, 59);

        String itemId = mockMvc.perform(post("/api/items")
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "title": "싱싱한 배추",
                                  "tradeType": "AUCTION",
                                  "conditionType": "NEW",
                                  "description": "오늘 수확한 배추입니다.",
                                  "initialPrice": 12000,
                                  "closeDate": "2026-06-30T23:59:59"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"data\":(\\d+).*", "$1");

        Long savedItemId = Long.valueOf(itemId);
        Item item = itemRepository.findById(savedItemId).orElseThrow();
        AuctionStatus auctionStatus = auctionStatusRepository.findById(savedItemId).orElseThrow();

        assertThat(item.getTitle()).isEqualTo("싱싱한 배추");
        assertThat(item.getTradeType()).isEqualTo(TradeType.AUCTION);
        assertThat(item.getConditionType()).isEqualTo(ConditionType.NEW);
        assertThat(item.getInitialPrice()).isEqualTo(12000L);
        assertThat(item.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
        assertThat(item.getIsDraft()).isFalse();
        assertThat(auctionStatus.getCurrentBid()).isEqualTo(12000L);
        assertThat(auctionStatus.getCloseDate()).isEqualTo(closeDate);

        Long sellerId = jdbcTemplate.queryForObject(
                "select seller_id from item where id = ?",
                Long.class,
                savedItemId);
        Long categoryId = jdbcTemplate.queryForObject(
                "select category_id from item where id = ?",
                Long.class,
                savedItemId);
        assertThat(sellerId).isEqualTo(seller.getId());
        assertThat(categoryId).isEqualTo(category.getId());
    }

    @DisplayName("인증 없이 상품 등록을 요청하면 401을 반환한다")
    @Test
    void 인증_없이_상품_등록을_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "title": "싱싱한 배추",
                                  "tradeType": "AUCTION",
                                  "conditionType": "NEW",
                                  "description": "오늘 수확한 배추입니다.",
                                  "initialPrice": 12000,
                                  "closeDate": "2026-06-30T23:59:59"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("인증 회원이 상품 좋아요를 처음 누르면 좋아요가 등록된다")
    @Test
    void 인증_회원이_상품_좋아요를_처음_누르면_좋아요가_등록된다() throws Exception {
        Client seller = saveClient("like-seller@example.com", "판매자");
        Client liker = saveClient("like-user@example.com", "좋아요회원");
        Item item = saveDirectItem(seller, "좋아요 상품");

        mockMvc.perform(post("/api/items/{itemId}/likes", item.getId())
                        .with(authentication(authenticationOf(liker))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        Long likeCount = jdbcTemplate.queryForObject(
                "select like_count from item where id = ?",
                Long.class,
                item.getId());
        Integer itemLikeCount = jdbcTemplate.queryForObject(
                "select count(*) from item_like where item_id = ? and client_id = ?",
                Integer.class,
                item.getId(),
                liker.getId());

        assertThat(likeCount).isEqualTo(1L);
        assertThat(itemLikeCount).isEqualTo(1);
    }

    @DisplayName("같은 회원이 상품 좋아요를 다시 누르면 좋아요가 취소된다")
    @Test
    void 같은_회원이_상품_좋아요를_다시_누르면_좋아요가_취소된다() throws Exception {
        Client seller = saveClient("toggle-seller@example.com", "판매자");
        Client liker = saveClient("toggle-user@example.com", "좋아요회원");
        Item item = saveDirectItem(seller, "토글 상품");

        mockMvc.perform(post("/api/items/{itemId}/likes", item.getId())
                        .with(authentication(authenticationOf(liker))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(true))
                .andExpect(jsonPath("$.data.likeCount").value(1));

        mockMvc.perform(post("/api/items/{itemId}/likes", item.getId())
                        .with(authentication(authenticationOf(liker))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.liked").value(false))
                .andExpect(jsonPath("$.data.likeCount").value(0));

        Long likeCount = jdbcTemplate.queryForObject(
                "select like_count from item where id = ?",
                Long.class,
                item.getId());
        Integer itemLikeCount = jdbcTemplate.queryForObject(
                "select count(*) from item_like where item_id = ? and client_id = ?",
                Integer.class,
                item.getId(),
                liker.getId());

        assertThat(likeCount).isEqualTo(0L);
        assertThat(itemLikeCount).isEqualTo(0);
    }

    @DisplayName("인증 없이 상품 좋아요를 요청하면 401을 반환한다")
    @Test
    void 인증_없이_상품_좋아요를_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(post("/api/items/{itemId}/likes", 1L))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("존재하지 않는 상품에 좋아요를 요청하면 ITEM_NOT_FOUND를 반환한다")
    @Test
    void 존재하지_않는_상품에_좋아요를_요청하면_ITEM_NOT_FOUND를_반환한다() throws Exception {
        Client liker = saveClient("missing-like-user@example.com", "좋아요회원");

        mockMvc.perform(post("/api/items/{itemId}/likes", 9999L)
                        .with(authentication(authenticationOf(liker))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
    }

    @DisplayName("임시저장 상품에 좋아요를 요청하면 ITEM_NOT_FOUND를 반환한다")
    @Test
    void 임시저장_상품에_좋아요를_요청하면_ITEM_NOT_FOUND를_반환한다() throws Exception {
        Client seller = saveClient("draft-like-seller@example.com", "판매자");
        Client liker = saveClient("draft-like-user@example.com", "좋아요회원");
        Category category = categoryRepository.save(Category.builder()
                .name("draft-like-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item draftItem = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("임시저장 좋아요 상품")
                .description("임시저장 상품")
                .initialPrice(1000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());

        mockMvc.perform(post("/api/items/{itemId}/likes", draftItem.getId())
                        .with(authentication(authenticationOf(liker))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));

        Long likeCount = jdbcTemplate.queryForObject(
                "select like_count from item where id = ?",
                Long.class,
                draftItem.getId());
        Integer itemLikeCount = jdbcTemplate.queryForObject(
                "select count(*) from item_like where item_id = ?",
                Integer.class,
                draftItem.getId());

        assertThat(likeCount).isEqualTo(0L);
        assertThat(itemLikeCount).isEqualTo(0);
    }

    @DisplayName("인증 회원이 상품을 임시저장하면 isDraft true인 Item과 AuctionStatus가 저장된다")
    @Test
    void authenticatedClientCanCreateItemDraft() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "draft-seller@example.com",
                passwordEncoder.encode("password123!"),
                "draftSeller",
                "draftSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("draft-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        LocalDateTime closeDate = LocalDateTime.of(2026, 7, 1, 10, 30, 0);

        String itemId = mockMvc.perform(post("/api/items/drafts")
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "title": "draft cabbage",
                                  "tradeType": "AUCTION",
                                  "conditionType": "USED",
                                  "description": "draft description",
                                  "initialPrice": 5000,
                                  "closeDate": "2026-07-01T10:30:00"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.itemId").isNumber())
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"itemId\":(\\d+).*", "$1");

        Long savedItemId = Long.valueOf(itemId);
        Item item = itemRepository.findById(savedItemId).orElseThrow();
        AuctionStatus auctionStatus = auctionStatusRepository.findById(savedItemId).orElseThrow();

        assertThat(item.getTitle()).isEqualTo("draft cabbage");
        assertThat(item.getTradeType()).isEqualTo(TradeType.AUCTION);
        assertThat(item.getConditionType()).isEqualTo(ConditionType.USED);
        assertThat(item.getInitialPrice()).isEqualTo(5000L);
        assertThat(item.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
        assertThat(item.getIsDraft()).isTrue();
        assertThat(auctionStatus.getCurrentBid()).isEqualTo(5000L);
        assertThat(auctionStatus.getCloseDate()).isEqualTo(closeDate);

        Long sellerId = jdbcTemplate.queryForObject(
                "select seller_id from item where id = ?",
                Long.class,
                savedItemId);
        Long categoryId = jdbcTemplate.queryForObject(
                "select category_id from item where id = ?",
                Long.class,
                savedItemId);
        assertThat(sellerId).isEqualTo(seller.getId());
        assertThat(categoryId).isEqualTo(category.getId());
    }

    @DisplayName("인증 없이 상품 임시저장을 요청하면 401을 반환한다")
    @Test
    void unauthenticatedClientCannotCreateItemDraft() throws Exception {
        mockMvc.perform(post("/api/items/drafts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "title": "draft cabbage",
                                  "tradeType": "AUCTION",
                                  "conditionType": "USED",
                                  "description": "draft description",
                                  "initialPrice": 5000,
                                  "closeDate": "2026-07-01T10:30:00"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("상품 임시저장 필수값이 없으면 400을 반환한다")
    @Test
    void itemDraftMissingRequiredFieldReturnsBadRequest() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "invalid-draft-seller@example.com",
                passwordEncoder.encode("password123!"),
                "invalidDraftSeller",
                "invalidDraftSeller",
                "010-1234-5678"));

        mockMvc.perform(post("/api/items/drafts")
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": 1,
                                  "tradeType": "AUCTION",
                                  "conditionType": "USED",
                                  "description": "draft description",
                                  "initialPrice": 5000,
                                  "closeDate": "2026-07-01T10:30:00"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @DisplayName("경매 임시저장에 closeDate가 없으면 AuctionStatus를 생성하지 않는다")
    @Test
    void auctionDraftWithoutCloseDateDoesNotCreateAuctionStatus() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "auction-draft-seller@example.com",
                passwordEncoder.encode("password123!"),
                "auctionDraftSeller",
                "auctionDraftSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("auction-draft-category")
                .sortOrder(1)
                .isActive(true)
                .build());

        String itemId = mockMvc.perform(post("/api/items/drafts")
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "title": "auction draft without close date",
                                  "tradeType": "AUCTION",
                                  "conditionType": "USED",
                                  "description": "draft description",
                                  "initialPrice": 5000
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.itemId").isNumber())
                .andReturn()
                .getResponse()
                .getContentAsString()
                .replaceAll(".*\"itemId\":(\\d+).*", "$1");

        Long savedItemId = Long.valueOf(itemId);
        Item item = itemRepository.findById(savedItemId).orElseThrow();

        assertThat(item.getTradeType()).isEqualTo(TradeType.AUCTION);
        assertThat(item.getIsDraft()).isTrue();
        assertThat(auctionStatusRepository.findById(savedItemId)).isEmpty();
    }

    @DisplayName("상품 목록 조회는 필터 조건에 맞는 공개 상품만 반환한다")
    @Test
    void getItemListReturnsOnlyVisibleItemsMatchingFilters() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "list-seller@example.com",
                passwordEncoder.encode("password123!"),
                "listSeller",
                "listSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("list-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Category otherCategory = categoryRepository.save(Category.builder()
                .name("other-list-category")
                .sortOrder(2)
                .isActive(true)
                .build());
        LocalDateTime closeDate = LocalDateTime.of(2026, 8, 1, 10, 0, 0);

        Item visibleItem = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("visible auction cabbage")
                .description("visible item")
                .initialPrice(7000L)
                .tradeType(TradeType.AUCTION)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());
        visibleItem.incrementLikeCount();
        itemRepository.saveAndFlush(visibleItem);
        auctionStatusRepository.save(AuctionStatus.builder()
                .item(visibleItem)
                .currentBid(9000L)
                .closeDate(closeDate)
                .build());
        itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("draft item")
                .description("draft item")
                .initialPrice(3000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());
        itemRepository.save(Item.builder()
                .seller(seller)
                .category(otherCategory)
                .title("other category item")
                .description("other category item")
                .initialPrice(4000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.NEW)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(get("/api/items")
                        .param("categoryId", String.valueOf(category.getId()))
                        .param("tradeStatus", "ON_SALE")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].itemId").value(visibleItem.getId()))
                .andExpect(jsonPath("$.data.content[0].title").value("visible auction cabbage"))
                .andExpect(jsonPath("$.data.content[0].initialPrice").value(7000))
                .andExpect(jsonPath("$.data.content[0].currentBid").value(9000))
                .andExpect(jsonPath("$.data.content[0].tradeStatus").value("ON_SALE"))
                .andExpect(jsonPath("$.data.content[0].likeCount").value(1))
                .andExpect(jsonPath("$.data.content[0].closeDate").value("2026-08-01T10:00:00"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }

    @DisplayName("상품 목록 조회 tradeStatus가 잘못되면 400을 반환한다")
    @Test
    void getItemListWithInvalidTradeStatusReturnsBadRequest() throws Exception {
        mockMvc.perform(get("/api/items")
                        .param("tradeStatus", "INVALID"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));
    }

    @DisplayName("상품 상세 조회는 조회수를 증가시킨 뒤 증가된 카운트를 응답한다")
    @Test
    void getItemDetailIncrementsViewCountAndReturnsUpdatedCount() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "detail-seller@example.com",
                passwordEncoder.encode("password123!"),
                "detailSeller",
                "detailSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("detail-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("visible detail item")
                .description("visible detail description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(get("/api/items/{itemId}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.title").value("visible detail item"))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.inquiryCount").value(0));

        Long firstViewCount = jdbcTemplate.queryForObject(
                "select view_count from item where id = ?",
                Long.class,
                item.getId());
        assertThat(firstViewCount).isEqualTo(1L);

        mockMvc.perform(get("/api/items/{itemId}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.viewCount").value(2));

        Long secondViewCount = jdbcTemplate.queryForObject(
                "select view_count from item where id = ?",
                Long.class,
                item.getId());
        assertThat(secondViewCount).isEqualTo(2L);
    }

    @DisplayName("상품 상세 조회는 임시저장 상품을 노출하지 않는다")
    @Test
    void getItemDetailDoesNotExposeDraftItems() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "draft-detail-seller@example.com",
                passwordEncoder.encode("password123!"),
                "draftDetailSeller",
                "draftDetailSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("draft-detail-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item draftItem = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("draft detail item")
                .description("draft detail description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());

        mockMvc.perform(get("/api/items/{itemId}", draftItem.getId()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));

        Long viewCount = jdbcTemplate.queryForObject(
                "select view_count from item where id = ?",
                Long.class,
                draftItem.getId());
        assertThat(viewCount).isEqualTo(0L);
    }

    @DisplayName("Item detail returns auction fields when auction status exists")
    @Test
    void getItemDetailReturnsAuctionFields() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "auction-detail-seller@example.com",
                passwordEncoder.encode("password123!"),
                "auctionDetailSeller",
                "auctionDetailSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("auction-detail-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("auction detail item")
                .description("auction detail description")
                .initialPrice(10000L)
                .tradeType(TradeType.AUCTION)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());
        LocalDateTime closeDate = LocalDateTime.of(2026, 8, 5, 15, 30, 0);
        auctionStatusRepository.save(AuctionStatus.builder()
                .item(item)
                .currentBid(15000L)
                .closeDate(closeDate)
                .build());

        mockMvc.perform(get("/api/items/{itemId}", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.title").value("auction detail item"))
                .andExpect(jsonPath("$.data.initialPrice").value(10000))
                .andExpect(jsonPath("$.data.currentBid").value(15000))
                .andExpect(jsonPath("$.data.tradeStatus").value("ON_SALE"))
                .andExpect(jsonPath("$.data.closeDate").value("2026-08-05T15:30:00"))
                .andExpect(jsonPath("$.data.viewCount").value(1))
                .andExpect(jsonPath("$.data.likeCount").value(0))
                .andExpect(jsonPath("$.data.inquiryCount").value(0));
    }

    @DisplayName("Item detail returns ITEM_NOT_FOUND for unknown item")
    @Test
    void getItemDetailReturnsNotFoundForUnknownItem() throws Exception {
        mockMvc.perform(get("/api/items/{itemId}", Long.MAX_VALUE))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
    }

    @DisplayName("직거래 상품은 경매 종료일 없이 상품 정보를 수정할 수 있다")
    @Test
    void updateDirectItemWithoutCloseDateSucceeds() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "direct-update-seller@example.com",
                passwordEncoder.encode("password123!"),
                "directUpdateSeller",
                "directUpdateSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("direct-update-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Category updatedCategory = categoryRepository.save(Category.builder()
                .name("direct-updated-category")
                .sortOrder(2)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("direct item")
                .description("direct description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(put("/api/items/{itemId}", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "title": "updated direct item",
                                  "description": "updated direct description",
                                  "initialPrice": 15000
                                }
                                """.formatted(updatedCategory.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getCategory().getId()).isEqualTo(updatedCategory.getId());
        assertThat(updatedItem.getTitle()).isEqualTo("updated direct item");
        assertThat(updatedItem.getDescription()).isEqualTo("updated direct description");
        assertThat(updatedItem.getInitialPrice()).isEqualTo(15000L);
        assertThat(auctionStatusRepository.findById(item.getId())).isEmpty();
    }

    @DisplayName("등록된 경매 상품은 상품 정보를 수정할 수 없다")
    @Test
    void updatePublishedAuctionItemReturnsBadRequest() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "auction-update-seller@example.com",
                passwordEncoder.encode("password123!"),
                "auctionUpdateSeller",
                "auctionUpdateSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("auction-update-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("auction item")
                .description("auction description")
                .initialPrice(10000L)
                .tradeType(TradeType.AUCTION)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());
        LocalDateTime originalCloseDate = LocalDateTime.of(2026, 8, 1, 10, 0, 0);
        auctionStatusRepository.save(AuctionStatus.builder()
                .item(item)
                .currentBid(10000L)
                .closeDate(originalCloseDate)
                .build());

        mockMvc.perform(put("/api/items/{itemId}", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "title": "updated auction item",
                                  "description": "updated auction description",
                                  "initialPrice": 20000,
                                  "closeDate": "2026-08-05T15:30:00"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("ITEM_UPDATE_NOT_ALLOWED"));

        Item notUpdatedItem = itemRepository.findById(item.getId()).orElseThrow();
        AuctionStatus auctionStatus = auctionStatusRepository.findById(item.getId()).orElseThrow();
        assertThat(notUpdatedItem.getInitialPrice()).isEqualTo(10000L);
        assertThat(auctionStatus.getCurrentBid()).isEqualTo(10000L);
        assertThat(auctionStatus.getCloseDate()).isEqualTo(originalCloseDate);
    }

    @DisplayName("임시저장 경매 상품은 상품 정보와 경매 상태를 함께 수정할 수 있다")
    @Test
    void updateDraftAuctionItemSynchronizesCurrentBid() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "draft-auction-update-seller@example.com",
                passwordEncoder.encode("password123!"),
                "draftAuctionSeller",
                "draftAuctionSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("draft-auction-update-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("draft auction item")
                .description("draft auction description")
                .initialPrice(10000L)
                .tradeType(TradeType.AUCTION)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());
        auctionStatusRepository.save(AuctionStatus.builder()
                .item(item)
                .currentBid(10000L)
                .closeDate(LocalDateTime.of(2026, 8, 1, 10, 0, 0))
                .build());

        mockMvc.perform(put("/api/items/{itemId}", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "title": "updated draft auction item",
                                  "description": "updated draft auction description",
                                  "initialPrice": 20000,
                                  "closeDate": "2026-08-05T15:30:00"
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        AuctionStatus auctionStatus = auctionStatusRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getInitialPrice()).isEqualTo(20000L);
        assertThat(updatedItem.getTitle()).isEqualTo("updated draft auction item");
        assertThat(auctionStatus.getCurrentBid()).isEqualTo(20000L);
        assertThat(auctionStatus.getCloseDate()).isEqualTo(LocalDateTime.of(2026, 8, 5, 15, 30, 0));
    }

    @DisplayName("상품 판매자가 아니면 상품을 수정할 수 없다")
    @Test
    void updateItemByNonSellerReturnsForbidden() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "forbidden-update-seller@example.com",
                passwordEncoder.encode("password123!"),
                "forbidSeller",
                "forbidSeller",
                "010-1234-5678"));
        Client otherClient = clientRepository.save(Client.create(
                "forbidden-update-other@example.com",
                passwordEncoder.encode("password123!"),
                "forbidOther",
                "forbidOther",
                "010-9876-5432"));
        Category category = categoryRepository.save(Category.builder()
                .name("forbidden-update-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("forbidden item")
                .description("forbidden description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(put("/api/items/{itemId}", item.getId())
                        .with(authentication(authenticationOf(otherClient)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "categoryId": %d,
                                  "title": "forbidden update",
                                  "description": "forbidden update description",
                                  "initialPrice": 15000
                                }
                                """.formatted(category.getId())))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @DisplayName("판매자는 등록된 상품의 판매 상태를 변경할 수 있다")
    @Test
    void 판매자는_등록된_상품의_판매_상태를_변경할_수_있다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "status-update-seller@example.com",
                passwordEncoder.encode("password123!"),
                "statusSeller",
                "statusSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("status-update-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("status item")
                .description("status description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        String responseBody = mockMvc.perform(patch("/api/items/{itemId}/status", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeStatus": "RESERVED"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.tradeStatus").value("RESERVED"))
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        LocalDateTime responseUpdatedAt = LocalDateTime.parse(
                objectMapper.readTree(responseBody).path("data").path("updatedAt").asText());
        long updatedAtDiffNanos = Math.abs(Duration.between(responseUpdatedAt, updatedItem.getUpdatedAt()).toNanos());

        assertThat(updatedItem.getTradeStatus()).isEqualTo(TradeStatus.RESERVED);
        assertThat(updatedAtDiffNanos).isLessThan(1_000_000L);
    }

    @DisplayName("직거래 상품을 판매완료로 변경할 때 구매자를 저장한다")
    @Test
    void 직거래_상품을_판매완료로_변경할_때_구매자를_저장한다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "direct-status-seller@example.com",
                passwordEncoder.encode("password123!"),
                "directSeller",
                "directSeller",
                "010-1234-5678"));
        Client buyer = clientRepository.save(Client.create(
                "direct-status-buyer@example.com",
                passwordEncoder.encode("password123!"),
                "directBuyer",
                "directBuyer",
                "010-9876-5432"));
        Category category = categoryRepository.save(Category.builder()
                .name("direct-status-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("direct status item")
                .description("direct status description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(patch("/api/items/{itemId}/status", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeStatus": "SOLD_OUT",
                                  "buyerId": %d
                                }
                                """.formatted(buyer.getId())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.tradeStatus").value("SOLD_OUT"))
                .andExpect(jsonPath("$.data.buyerId").value(buyer.getId()))
                .andExpect(jsonPath("$.data.updatedAt").exists());

        Item updatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(updatedItem.getTradeStatus()).isEqualTo(TradeStatus.SOLD_OUT);
        assertThat(updatedItem.getBuyer().getId()).isEqualTo(buyer.getId());
    }

    @DisplayName("직거래 상품을 판매완료로 변경할 때 구매자가 없으면 400을 반환한다")
    @Test
    void 직거래_상품을_판매완료로_변경할_때_구매자가_없으면_400을_반환한다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "direct-status-missing-seller@example.com",
                passwordEncoder.encode("password123!"),
                "directMissingSeller",
                "directMissingSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("direct-status-missing-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("direct status missing item")
                .description("direct status missing description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(patch("/api/items/{itemId}/status", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeStatus": "SOLD_OUT"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        Item notUpdatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(notUpdatedItem.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
        assertThat(notUpdatedItem.getBuyer()).isNull();
    }

    @DisplayName("직거래 상품을 판매완료로 변경할 때 판매자를 구매자로 지정하면 400을 반환한다")
    @Test
    void 직거래_상품을_판매완료로_변경할_때_판매자를_구매자로_지정하면_400을_반환한다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "direct-status-self-seller@example.com",
                passwordEncoder.encode("password123!"),
                "directSelfSeller",
                "directSelfSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("direct-status-self-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("direct status self item")
                .description("direct status self description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(patch("/api/items/{itemId}/status", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeStatus": "SOLD_OUT",
                                  "buyerId": %d
                                }
                                """.formatted(seller.getId())))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        Item notUpdatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(notUpdatedItem.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
        assertThat(notUpdatedItem.getBuyer()).isNull();
    }

    @DisplayName("직거래 상품을 판매완료로 변경할 때 구매자가 존재하지 않으면 404를 반환한다")
    @Test
    void 직거래_상품을_판매완료로_변경할_때_구매자가_존재하지_않으면_404를_반환한다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "direct-status-not-found-seller@example.com",
                passwordEncoder.encode("password123!"),
                "directNotFoundSeller",
                "directNotFoundSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("direct-status-not-found-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("direct status not found item")
                .description("direct status not found description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(patch("/api/items/{itemId}/status", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeStatus": "SOLD_OUT",
                                  "buyerId": 999999
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));

        Item notUpdatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(notUpdatedItem.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
        assertThat(notUpdatedItem.getBuyer()).isNull();
    }

    @DisplayName("상품 판매자가 아니면 판매 상태를 변경할 수 없다")
    @Test
    void 상품_판매자가_아니면_판매_상태를_변경할_수_없다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "status-forbidden-seller@example.com",
                passwordEncoder.encode("password123!"),
                "statusForbidSeller",
                "statusForbidSeller",
                "010-1234-5678"));
        Client otherClient = clientRepository.save(Client.create(
                "status-forbidden-other@example.com",
                passwordEncoder.encode("password123!"),
                "statusForbiddenOther",
                "statusForbiddenOther",
                "010-9876-5432"));
        Category category = categoryRepository.save(Category.builder()
                .name("status-forbidden-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("status forbidden item")
                .description("status forbidden description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(patch("/api/items/{itemId}/status", item.getId())
                        .with(authentication(authenticationOf(otherClient)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeStatus": "RESERVED"
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        Item notUpdatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(notUpdatedItem.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
    }

    @DisplayName("임시저장 상품은 판매 상태를 변경할 수 없다")
    @Test
    void 임시저장_상품은_판매_상태를_변경할_수_없다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "status-draft-seller@example.com",
                passwordEncoder.encode("password123!"),
                "statusDraftSeller",
                "statusDraftSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("status-draft-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("status draft item")
                .description("status draft description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());

        mockMvc.perform(patch("/api/items/{itemId}/status", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeStatus": "RESERVED"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("ITEM_STATUS_UPDATE_NOT_ALLOWED"));

        Item notUpdatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(notUpdatedItem.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
    }

    @DisplayName("정의되지 않은 판매 상태를 요청하면 INVALID_INPUT을 반환한다")
    @Test
    void 정의되지_않은_판매_상태를_요청하면_INVALID_INPUT을_반환한다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "status-invalid-seller@example.com",
                passwordEncoder.encode("password123!"),
                "statusInvalidSeller",
                "statusInvalidSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("status-invalid-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("status invalid item")
                .description("status invalid description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(patch("/api/items/{itemId}/status", item.getId())
                        .with(authentication(authenticationOf(seller)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "tradeStatus": "INVALID"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("INVALID_INPUT"));

        Item notUpdatedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(notUpdatedItem.getTradeStatus()).isEqualTo(TradeStatus.ON_SALE);
    }

    @DisplayName("판매자는 임시저장 상품을 게시할 수 있다")
    @Test
    void 판매자는_임시저장_상품을_게시할_수_있다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "publish-seller@example.com",
                passwordEncoder.encode("password123!"),
                "publishSeller",
                "publishSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("publish-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("publish item")
                .description("publish description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());

        String responseBody = mockMvc.perform(post("/api/items/{itemId}/publish", item.getId())
                        .with(authentication(authenticationOf(seller))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.updatedAt").exists())
                .andReturn()
                .getResponse()
                .getContentAsString();

        Item publishedItem = itemRepository.findById(item.getId()).orElseThrow();
        LocalDateTime responseUpdatedAt = LocalDateTime.parse(
                objectMapper.readTree(responseBody).path("data").path("updatedAt").asText());
        long updatedAtDiffNanos = Math.abs(Duration.between(responseUpdatedAt, publishedItem.getUpdatedAt()).toNanos());

        assertThat(publishedItem.getIsDraft()).isFalse();
        assertThat(updatedAtDiffNanos).isLessThan(1_000_000L);
    }

    @DisplayName("상품 판매자가 아니면 임시저장 상품을 게시할 수 없다")
    @Test
    void 상품_판매자가_아니면_임시저장_상품을_게시할_수_없다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "publish-forbidden-seller@example.com",
                passwordEncoder.encode("password123!"),
                "publishForbidSeller",
                "publishForbidSeller",
                "010-1234-5678"));
        Client otherClient = clientRepository.save(Client.create(
                "publish-forbidden-other@example.com",
                passwordEncoder.encode("password123!"),
                "publishForbidOther",
                "publishForbidOther",
                "010-9876-5432"));
        Category category = categoryRepository.save(Category.builder()
                .name("publish-forbidden-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("publish forbidden item")
                .description("publish forbidden description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());

        mockMvc.perform(post("/api/items/{itemId}/publish", item.getId())
                        .with(authentication(authenticationOf(otherClient))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        Item notPublishedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(notPublishedItem.getIsDraft()).isTrue();
    }

    @DisplayName("이미 게시된 상품을 다시 게시할 수 없다")
    @Test
    void 이미_게시된_상품을_다시_게시할_수_없다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "publish-already-seller@example.com",
                passwordEncoder.encode("password123!"),
                "publishAlreadySeller",
                "publishAlreadySeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("publish-already-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("already published item")
                .description("already published description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(post("/api/items/{itemId}/publish", item.getId())
                        .with(authentication(authenticationOf(seller))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("ITEM_PUBLISH_NOT_ALLOWED"));
    }

    @DisplayName("경매 상태가 없는 임시저장 경매 상품은 게시할 수 없다")
    @Test
    void 경매_상태가_없는_임시저장_경매_상품은_게시할_수_없다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "publish-auction-missing-seller@example.com",
                passwordEncoder.encode("password123!"),
                "pubAuctionMissing",
                "pubAuctionMissing",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("publish-auction-missing-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("auction missing status item")
                .description("auction missing status description")
                .initialPrice(10000L)
                .tradeType(TradeType.AUCTION)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());

        mockMvc.perform(post("/api/items/{itemId}/publish", item.getId())
                        .with(authentication(authenticationOf(seller))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("AUCTION_STATUS_NOT_FOUND"));

        Item notPublishedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(notPublishedItem.getIsDraft()).isTrue();
    }

    @DisplayName("경매 종료일이 지난 임시저장 경매 상품은 게시할 수 없다")
    @Test
    void 경매_종료일이_지난_임시저장_경매_상품은_게시할_수_없다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "publish-auction-closed-seller@example.com",
                passwordEncoder.encode("password123!"),
                "publishAuctionClosed",
                "publishAuctionClosed",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("publish-auction-closed-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("auction closed item")
                .description("auction closed description")
                .initialPrice(10000L)
                .tradeType(TradeType.AUCTION)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());
        auctionStatusRepository.save(AuctionStatus.builder()
                .item(item)
                .currentBid(10000L)
                .closeDate(LocalDateTime.of(2020, 1, 1, 10, 0, 0))
                .build());

        mockMvc.perform(post("/api/items/{itemId}/publish", item.getId())
                        .with(authentication(authenticationOf(seller))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("AUCTION_ALREADY_CLOSED"));

        Item notPublishedItem = itemRepository.findById(item.getId()).orElseThrow();
        assertThat(notPublishedItem.getIsDraft()).isTrue();
    }

    @DisplayName("판매자는 임시저장 상품을 하드 삭제할 수 있다")
    @Test
    void 판매자는_임시저장_상품을_하드_삭제할_수_있다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "draft-delete-seller@example.com",
                passwordEncoder.encode("password123!"),
                "draftDeleteSeller",
                "draftDeleteSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("draft-delete-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("draft delete item")
                .description("draft delete description")
                .initialPrice(10000L)
                .tradeType(TradeType.AUCTION)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(true)
                .build());
        auctionStatusRepository.save(AuctionStatus.builder()
                .item(item)
                .currentBid(10000L)
                .closeDate(LocalDateTime.of(2026, 8, 1, 10, 0, 0))
                .build());

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .with(authentication(authenticationOf(seller))))
                .andExpect(status().isNoContent());

        Integer itemCount = jdbcTemplate.queryForObject(
                "select count(*) from item where id = ?",
                Integer.class,
                item.getId());
        Integer auctionStatusCount = jdbcTemplate.queryForObject(
                "select count(*) from auction_status where item_id = ?",
                Integer.class,
                item.getId());

        assertThat(itemCount).isZero();
        assertThat(auctionStatusCount).isZero();
    }

    @DisplayName("판매자는 게시된 상품을 소프트 삭제할 수 있다")
    @Test
    void 판매자는_게시된_상품을_소프트_삭제할_수_있다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "published-delete-seller@example.com",
                passwordEncoder.encode("password123!"),
                "pubDeleteSeller",
                "pubDeleteSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("published-delete-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("published delete item")
                .description("published delete description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .with(authentication(authenticationOf(seller))))
                .andExpect(status().isNoContent());

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "select is_deleted from item where id = ?",
                Boolean.class,
                item.getId());
        Integer itemCount = jdbcTemplate.queryForObject(
                "select count(*) from item where id = ?",
                Integer.class,
                item.getId());

        assertThat(itemCount).isOne();
        assertThat(isDeleted).isTrue();
        assertThat(itemRepository.findById(item.getId())).isEmpty();
    }

    @DisplayName("상품 판매자가 아니면 상품을 삭제할 수 없다")
    @Test
    void 상품_판매자가_아니면_상품을_삭제할_수_없다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "delete-forbidden-seller@example.com",
                passwordEncoder.encode("password123!"),
                "deleteForbidSeller",
                "deleteForbidSeller",
                "010-1234-5678"));
        Client otherClient = clientRepository.save(Client.create(
                "delete-forbidden-other@example.com",
                passwordEncoder.encode("password123!"),
                "deleteForbidOther",
                "deleteForbidOther",
                "010-9876-5432"));
        Category category = categoryRepository.save(Category.builder()
                .name("delete-forbidden-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("delete forbidden item")
                .description("delete forbidden description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .with(authentication(authenticationOf(otherClient))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "select is_deleted from item where id = ?",
                Boolean.class,
                item.getId());

        assertThat(isDeleted).isFalse();
    }

    @DisplayName("이미 삭제된 상품을 삭제하면 ITEM_NOT_FOUND를 반환한다")
    @Test
    void 이미_삭제된_상품을_삭제하면_ITEM_NOT_FOUND를_반환한다() throws Exception {
        Client seller = clientRepository.save(Client.create(
                "already-delete-seller@example.com",
                passwordEncoder.encode("password123!"),
                "alreadyDelSeller",
                "alreadyDelSeller",
                "010-1234-5678"));
        Category category = categoryRepository.save(Category.builder()
                .name("already-delete-category")
                .sortOrder(1)
                .isActive(true)
                .build());
        Item item = itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title("already delete item")
                .description("already delete description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());
        jdbcTemplate.update("update item set is_deleted = true where id = ?", item.getId());

        mockMvc.perform(delete("/api/items/{itemId}", item.getId())
                        .with(authentication(authenticationOf(seller))))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
    }

    private Client saveClient(String email, String nickname) {
        return clientRepository.save(Client.create(
                email,
                passwordEncoder.encode("password123!"),
                nickname,
                nickname,
                "010-1234-5678"));
    }

    private Item saveDirectItem(Client seller, String title) {
        Category category = categoryRepository.save(Category.builder()
                .name("like-category-" + title)
                .sortOrder(1)
                .isActive(true)
                .build());
        return itemRepository.save(Item.builder()
                .seller(seller)
                .category(category)
                .title(title)
                .description(title + " description")
                .initialPrice(10000L)
                .tradeType(TradeType.DIRECT)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());
    }

    private UsernamePasswordAuthenticationToken authenticationOf(Client seller) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedClient(seller.getId(), seller.getEmail()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}

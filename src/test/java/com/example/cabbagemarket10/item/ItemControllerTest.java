package com.example.cabbagemarket10.item;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import java.time.LocalDateTime;
import java.util.List;
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

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from inquiry_log");
        jdbcTemplate.update("delete from auction_status");
        jdbcTemplate.update("delete from item");
        jdbcTemplate.update("delete from category");
        jdbcTemplate.update("delete from client");
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

    private UsernamePasswordAuthenticationToken authenticationOf(Client seller) {
        return new UsernamePasswordAuthenticationToken(
                new AuthenticatedClient(seller.getId(), seller.getEmail()),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }
}

package com.example.cabbagemarket10.domain.review.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import com.example.cabbagemarket10.domain.review.entity.Review;
import com.example.cabbagemarket10.domain.review.repository.ReviewRepository;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import java.time.LocalDateTime;
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

@SpringBootTest
@AutoConfigureMockMvc
class ReviewControllerTest {

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
    private ReviewRepository reviewRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
    }

    @DisplayName("낙찰자는 거래완료 상품에 리뷰를 생성할 수 있다")
    @Test
    void 낙찰자는_거래완료_상품에_리뷰를_생성할_수_있다() throws Exception {
        Client seller = saveClient("seller@example.com", "판매자", "김판매");
        Client buyer = saveClient("buyer@example.com", "구매자", "박구매");
        Item item = saveAuctionItem(seller, TradeStatus.SOLD_OUT);
        saveAuctionStatus(item, buyer);
        String accessToken = jwtTokenProvider.createAccessToken(buyer);

        mockMvc.perform(post("/api/items/{itemId}/reviews", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5,
                                  "content": "좋은 거래였습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.reviewId").exists())
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.reviewerId").value(buyer.getId()))
                .andExpect(jsonPath("$.data.revieweeId").value(seller.getId()))
                .andExpect(jsonPath("$.data.rating").value(5))
                .andExpect(jsonPath("$.data.content").value("좋은 거래였습니다."))
                .andExpect(jsonPath("$.data.date").exists());

        Review review = reviewRepository.findAll().get(0);
        assertThat(review.getItem().getId()).isEqualTo(item.getId());
        assertThat(review.getReviewer().getId()).isEqualTo(buyer.getId());
        assertThat(review.getReviewee().getId()).isEqualTo(seller.getId());
    }

    @DisplayName("직거래 구매자는 거래완료 상품에 리뷰를 생성할 수 있다")
    @Test
    void 직거래_구매자는_거래완료_상품에_리뷰를_생성할_수_있다() throws Exception {
        Client seller = saveClient("direct-seller@example.com", "직거래판매자", "김판매");
        Client buyer = saveClient("direct-buyer@example.com", "직거래구매자", "박구매");
        Item item = saveDirectSoldOutItem(seller, buyer);
        String accessToken = jwtTokenProvider.createAccessToken(buyer);

        mockMvc.perform(post("/api/items/{itemId}/reviews", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5,
                                  "content": "좋은 직거래였습니다."
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.reviewerId").value(buyer.getId()))
                .andExpect(jsonPath("$.data.revieweeId").value(seller.getId()));
    }

    @DisplayName("낙찰자가 아니면 리뷰 생성 시 403을 반환한다")
    @Test
    void 낙찰자가_아니면_리뷰_생성_시_403을_반환한다() throws Exception {
        Client seller = saveClient("seller@example.com", "판매자", "김판매");
        Client buyer = saveClient("buyer@example.com", "구매자", "박구매");
        Client other = saveClient("other@example.com", "타인", "최타인");
        Item item = saveAuctionItem(seller, TradeStatus.SOLD_OUT);
        saveAuctionStatus(item, buyer);
        String accessToken = jwtTokenProvider.createAccessToken(other);

        mockMvc.perform(post("/api/items/{itemId}/reviews", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5,
                                  "content": "좋은 거래였습니다."
                                }
                                """))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("REVIEW_NOT_ALLOWED"));
    }

    @DisplayName("이미 리뷰를 작성한 상품이면 409를 반환한다")
    @Test
    void 이미_리뷰를_작성한_상품이면_409를_반환한다() throws Exception {
        Client seller = saveClient("seller@example.com", "판매자", "김판매");
        Client buyer = saveClient("buyer@example.com", "구매자", "박구매");
        Item item = saveAuctionItem(seller, TradeStatus.SOLD_OUT);
        saveAuctionStatus(item, buyer);
        reviewRepository.save(Review.builder()
                .item(item)
                .reviewer(buyer)
                .reviewee(seller)
                .rating(4)
                .content("기존 리뷰")
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(buyer);

        mockMvc.perform(post("/api/items/{itemId}/reviews", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5,
                                  "content": "좋은 거래였습니다."
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("REVIEW_ALREADY_EXISTS"));
    }

    @DisplayName("상품이 없으면 리뷰 생성 시 404를 반환한다")
    @Test
    void 상품이_없으면_리뷰_생성_시_404를_반환한다() throws Exception {
        Client buyer = saveClient("buyer@example.com", "구매자", "박구매");
        String accessToken = jwtTokenProvider.createAccessToken(buyer);

        mockMvc.perform(post("/api/items/{itemId}/reviews", 9999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5,
                                  "content": "좋은 거래였습니다."
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
    }

    @DisplayName("평점이 1점 미만이면 리뷰 생성 시 400을 반환한다")
    @Test
    void 평점이_1점_미만이면_리뷰_생성_시_400을_반환한다() throws Exception {
        Client buyer = saveClient("rating-buyer@example.com", "평점구매자", "박구매");
        String accessToken = jwtTokenProvider.createAccessToken(buyer);

        mockMvc.perform(post("/api/items/{itemId}/reviews", 9999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 0,
                                  "content": "좋은 거래였습니다."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("리뷰 내용이 500자를 초과하면 리뷰 생성 시 400을 반환한다")
    @Test
    void 리뷰_내용이_500자를_초과하면_리뷰_생성_시_400을_반환한다() throws Exception {
        Client buyer = saveClient("content-buyer@example.com", "내용구매자", "박구매");
        String accessToken = jwtTokenProvider.createAccessToken(buyer);
        String content = "a".repeat(501);

        mockMvc.perform(post("/api/items/{itemId}/reviews", 9999L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "rating": 5,
                                  "content": "%s"
                                }
                                """.formatted(content)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("비회원도 특정 회원이 받은 리뷰 목록을 조회할 수 있다")
    @Test
    void 비회원도_특정_회원이_받은_리뷰_목록을_조회할_수_있다() throws Exception {
        Client seller = saveClient("received-seller@example.com", "받은리뷰판매자", "김판매");
        Client buyer = saveClient("received-buyer@example.com", "받은리뷰구매자", "박구매");
        Item item = saveAuctionItem(seller, TradeStatus.SOLD_OUT);
        Review review = reviewRepository.save(Review.builder()
                .item(item)
                .reviewer(buyer)
                .reviewee(seller)
                .rating(5)
                .content("친절한 판매자였습니다.")
                .build());

        mockMvc.perform(get("/api/clients/{clientId}/reviews", seller.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].reviewId").value(review.getId()))
                .andExpect(jsonPath("$.data.content[0].itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.content[0].reviewerId").value(buyer.getId()))
                .andExpect(jsonPath("$.data.content[0].reviewerNickname").value("받은리뷰구매자"))
                .andExpect(jsonPath("$.data.content[0].reviewerProfileImageUrl")
                        .value(Client.defaultProfileImageUrl()))
                .andExpect(jsonPath("$.data.content[0].rating").value(5))
                .andExpect(jsonPath("$.data.content[0].content").value("친절한 판매자였습니다."))
                .andExpect(jsonPath("$.data.content[0].createdAt").exists())
                .andExpect(jsonPath("$.data.content[0].revieweeId").doesNotExist());
    }

    @DisplayName("존재하지 않는 회원의 받은 리뷰 목록을 조회하면 404를 반환한다")
    @Test
    void 존재하지_않는_회원의_받은_리뷰_목록을_조회하면_404를_반환한다() throws Exception {
        mockMvc.perform(get("/api/clients/{clientId}/reviews", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    @DisplayName("삭제된 리뷰는 받은 리뷰 목록에서 제외된다")
    @Test
    void 삭제된_리뷰는_받은_리뷰_목록에서_제외된다() throws Exception {
        Client seller = saveClient("deleted-review-seller@example.com", "삭제리뷰판매자", "김판매");
        Client buyer = saveClient("deleted-review-buyer@example.com", "삭제리뷰구매자", "박구매");
        Item remainingItem = saveAuctionItem(seller, TradeStatus.SOLD_OUT);
        Item deletedItem = saveAuctionItem(seller, TradeStatus.SOLD_OUT);
        Review remainingReview = reviewRepository.save(Review.builder()
                .item(remainingItem)
                .reviewer(buyer)
                .reviewee(seller)
                .rating(4)
                .content("남는 리뷰")
                .build());
        Review deletedReview = reviewRepository.save(Review.builder()
                .item(deletedItem)
                .reviewer(buyer)
                .reviewee(seller)
                .rating(2)
                .content("삭제될 리뷰")
                .build());
        reviewRepository.delete(deletedReview);
        reviewRepository.flush();

        mockMvc.perform(get("/api/clients/{clientId}/reviews", seller.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].reviewId").value(remainingReview.getId()));
    }

    @DisplayName("받은 리뷰 목록은 최신순으로 정렬된다")
    @Test
    void 받은_리뷰_목록은_최신순으로_정렬된다() throws Exception {
        Client seller = saveClient("sort-seller@example.com", "정렬판매자", "김판매");
        Client buyer = saveClient("sort-buyer@example.com", "정렬구매자", "박구매");
        Item olderItem = saveAuctionItem(seller, TradeStatus.SOLD_OUT);
        Item newerItem = saveAuctionItem(seller, TradeStatus.SOLD_OUT);
        Review olderReview = reviewRepository.save(Review.builder()
                .item(olderItem)
                .reviewer(buyer)
                .reviewee(seller)
                .rating(3)
                .content("먼저 작성된 리뷰")
                .build());
        Review newerReview = reviewRepository.save(Review.builder()
                .item(newerItem)
                .reviewer(buyer)
                .reviewee(seller)
                .rating(5)
                .content("나중에 작성된 리뷰")
                .build());

        mockMvc.perform(get("/api/clients/{clientId}/reviews", seller.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].reviewId").value(newerReview.getId()))
                .andExpect(jsonPath("$.data.content[1].reviewId").value(olderReview.getId()));
    }

    private Client saveClient(String email, String nickname, String name) {
        return clientRepository.save(Client.create(
                email,
                passwordEncoder.encode("password123!"),
                nickname,
                name,
                "010-1234-5678"));
    }

    private Item saveAuctionItem(Client seller, TradeStatus tradeStatus) {
        Category category = categoryRepository.save(Category.builder()
                .name("디지털/가전-" + System.nanoTime())
                .sortOrder(1)
                .isActive(true)
                .build());

        return itemRepository.save(Item.builder()
                .category(category)
                .seller(seller)
                .tradeType(TradeType.AUCTION)
                .title("중고 노트북")
                .description("상태 좋은 노트북입니다.")
                .initialPrice(100_000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(tradeStatus)
                .isDraft(false)
                .build());
    }

    private Item saveDirectSoldOutItem(Client seller, Client buyer) {
        Category category = categoryRepository.save(Category.builder()
                .name("직거래-" + System.nanoTime())
                .sortOrder(1)
                .isActive(true)
                .build());

        Item item = Item.builder()
                .category(category)
                .seller(seller)
                .tradeType(TradeType.DIRECT)
                .title("직거래 상품")
                .description("직거래 상품입니다.")
                .initialPrice(100_000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.SOLD_OUT)
                .isDraft(false)
                .build();
        item.updateStatus(TradeStatus.SOLD_OUT, buyer);
        return itemRepository.saveAndFlush(item);
    }

    private AuctionStatus saveAuctionStatus(Item item, Client buyer) {
        AuctionStatus auctionStatus = AuctionStatus.builder()
                .item(item)
                .currentBid(100_000L)
                .closeDate(LocalDateTime.now().plusDays(1))
                .build();
        auctionStatus.updateBid(110_000L, buyer.getId(), LocalDateTime.now());
        return auctionStatusRepository.saveAndFlush(auctionStatus);
    }

    private void deleteIfExists(String tableName) {
        Integer tableCount = jdbcTemplate.queryForObject(
                "select count(*) from information_schema.tables where table_name = ?",
                Integer.class,
                tableName);

        if (tableCount != null && tableCount > 0) {
            jdbcTemplate.update("delete from " + tableName);
        }
    }
}

package com.example.cabbagemarket10.domain.client.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.follow.entity.Follow;
import com.example.cabbagemarket10.domain.follow.repository.FollowRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.domain.itemImage.entity.ItemImage;
import com.example.cabbagemarket10.domain.itemImage.repository.ItemImageRepository;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import com.example.cabbagemarket10.domain.review.entity.Review;
import com.example.cabbagemarket10.domain.review.repository.ReviewRepository;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
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
class ClientControllerTest {

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
    private FollowRepository followRepository;

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
        deleteIfExists("follow");
        deleteIfExists("review");
        deleteIfExists("auction_status");
        deleteIfExists("item_image");
        deleteIfExists("item");
        deleteIfExists("category");
        deleteIfExists("client");
    }

    @DisplayName("인증된 회원은 내 정보를 조회할 수 있다")
    @Test
    void 인증된_회원은_내_정보를_조회할_수_있다() throws Exception {
        Client client = saveClient(
                "me@example.com",
                "cabbage",
                "홍길동",
                "010-1234-5678");
        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(get("/api/clients/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.clientId").value(client.getId()))
                .andExpect(jsonPath("$.data.email").value("me@example.com"))
                .andExpect(jsonPath("$.data.nickname").value("cabbage"))
                .andExpect(jsonPath("$.data.name").value("홍길동"));
    }

    @DisplayName("내 정보 조회 응답에는 password가 포함되지 않는다")
    @Test
    void 내_정보_조회_응답에는_password가_포함되지_않는다() throws Exception {
        Client client = saveClient(
                "secure@example.com",
                "securecabbage",
                "김보안",
                "010-1111-2222");
        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(get("/api/clients/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.password").doesNotExist());
    }

    @DisplayName("토큰 없이 내 정보 조회를 요청하면 401을 반환한다")
    @Test
    void 토큰_없이_내_정보_조회를_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/clients/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }


    @DisplayName("인증된 회원은 자신의 판매글 목록을 조회할 수 있다")
    @Test
    void 인증된_회원은_자신의_판매글_목록을_조회할_수_있다() throws Exception {
        Client client = saveClient(
                "myitems@example.com",
                "내판매글회원",
                "홍길동",
                "010-1234-5678");
        Category category = saveCategory();
        Item onSaleItem = saveItem(category, client, "판매중 상품", TradeStatus.ON_SALE, false);
        Item draftItem = saveItem(category, client, "임시저장 상품", TradeStatus.ON_SALE, true);
        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(get("/api/clients/me/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.content[*].itemId")
                        .value(org.hamcrest.Matchers.containsInAnyOrder(
                                onSaleItem.getId().intValue(), draftItem.getId().intValue())));
    }

    @DisplayName("내 판매글 목록 응답에는 대표 이미지, 카테고리, 좋아요 수, 거래 방식, 상품 상태가 포함된다")
    @Test
    void 내_판매글_목록_응답에는_대표_이미지_카테고리_좋아요_수_거래_방식_상품_상태가_포함된다() throws Exception {
        Client client = saveClient(
                "richitem@example.com",
                "상세필드회원",
                "홍길동",
                "010-6666-7777");
        Category category = saveCategory();
        Item item = itemRepository.save(Item.builder()
                .category(category)
                .seller(client)
                .tradeType(TradeType.AUCTION)
                .title("경매 상품")
                .description("설명")
                .initialPrice(10000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());
        itemImageRepository.save(ItemImage.builder()
                .item(item)
                .imageUrl("https://example.com/items/thumbnail.jpg")
                .sortOrder(0)
                .isThumbnail(true)
                .build());
        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(get("/api/clients/me/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].itemId").value(item.getId()))
                .andExpect(jsonPath("$.data.content[0].tradeType").value("AUCTION"))
                .andExpect(jsonPath("$.data.content[0].conditionType").value("USED"))
                .andExpect(jsonPath("$.data.content[0].likeCount").value(0))
                .andExpect(jsonPath("$.data.content[0].categoryId").value(category.getId()))
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl")
                        .value("https://example.com/items/thumbnail.jpg"));
    }

    @DisplayName("대표 이미지가 없는 상품은 목록에서 thumbnailUrl이 null이다")
    @Test
    void 대표_이미지가_없는_상품은_목록에서_thumbnailUrl이_null이다() throws Exception {
        Client client = saveClient(
                "noimage@example.com",
                "이미지없는회원",
                "홍길동",
                "010-7777-8888");
        Category category = saveCategory();
        saveItem(category, client, "이미지 없는 상품", TradeStatus.ON_SALE, false);
        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(get("/api/clients/me/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].thumbnailUrl")
                        .value(org.hamcrest.Matchers.nullValue()));
    }

    @DisplayName("다른 회원의 판매글은 내 판매글 목록에 포함되지 않는다")
    @Test
    void 다른_회원의_판매글은_내_판매글_목록에_포함되지_않는다() throws Exception {
        Client owner = saveClient(
                "owner@example.com",
                "소유자",
                "소유",
                "010-1111-2222");
        Client other = saveClient(
                "other@example.com",
                "다른회원",
                "다른",
                "010-2222-3333");
        Category category = saveCategory();
        Item ownItem = saveItem(category, owner, "내 상품", TradeStatus.ON_SALE, false);
        saveItem(category, other, "다른 회원 상품", TradeStatus.ON_SALE, false);
        String accessToken = jwtTokenProvider.createAccessToken(owner);

        mockMvc.perform(get("/api/clients/me/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].itemId").value(ownItem.getId()));
    }

    @DisplayName("삭제된 상품은 내 판매글 목록에서 제외된다")
    @Test
    void 삭제된_상품은_내_판매글_목록에서_제외된다() throws Exception {
        Client client = saveClient(
                "deleteitem@example.com",
                "삭제상품회원",
                "홍길동",
                "010-3333-4444");
        Category category = saveCategory();
        Item remainingItem = saveItem(category, client, "남는 상품", TradeStatus.ON_SALE, false);
        Item deletedItem = saveItem(category, client, "삭제될 상품", TradeStatus.ON_SALE, false);
        itemRepository.delete(deletedItem);
        itemRepository.flush();
        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(get("/api/clients/me/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].itemId").value(remainingItem.getId()));
    }

    @DisplayName("tradeStatus로 내 판매글 목록을 필터링할 수 있다")
    @Test
    void tradeStatus로_내_판매글_목록을_필터링할_수_있다() throws Exception {
        Client client = saveClient(
                "filteritem@example.com",
                "필터상품회원",
                "홍길동",
                "010-4444-5555");
        Category category = saveCategory();
        saveItem(category, client, "판매중 상품", TradeStatus.ON_SALE, false);
        Item soldItem = saveItem(category, client, "판매완료 상품", TradeStatus.SOLD_OUT, false);
        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(get("/api/clients/me/items")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .param("tradeStatus", "SOLD_OUT"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].itemId").value(soldItem.getId()));
    }

    @DisplayName("토큰 없이 내 판매글 목록 조회를 요청하면 401을 반환한다")
    @Test
    void 토큰_없이_내_판매글_목록_조회를_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(get("/api/clients/me/items"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("비회원도 회원 공개 프로필을 조회할 수 있다")
    @Test
    void 비회원도_회원_공개_프로필을_조회할_수_있다() throws Exception {
        Client client = saveClient(
                "public@example.com",
                "공개배추",
                "공개이름",
                "010-2222-3333");

        mockMvc.perform(get("/api/clients/{clientId}", client.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.clientId").value(client.getId()))
                .andExpect(jsonPath("$.data.nickname").value("공개배추"))
                .andExpect(jsonPath("$.data.profileImageUrl").value(Client.defaultProfileImageUrl()))
                .andExpect(jsonPath("$.data.averageRating").value(0.0))
                .andExpect(jsonPath("$.data.reviewCount").value(0))
                .andExpect(jsonPath("$.data.followerCount").value(0))
                .andExpect(jsonPath("$.data.isFollowing").value(false))
                .andExpect(jsonPath("$.data.sellingItemCount").value(0))
                .andExpect(jsonPath("$.data.soldItemCount").value(0))
                .andExpect(jsonPath("$.data.email").doesNotExist())
                .andExpect(jsonPath("$.data.password").doesNotExist())
                .andExpect(jsonPath("$.data.status").doesNotExist())
                .andExpect(jsonPath("$.data.phone").doesNotExist());
    }

    @DisplayName("회원도 회원 공개 프로필을 조회할 수 있다")
    @Test
    void 회원도_회원_공개_프로필을_조회할_수_있다() throws Exception {
        Client client = saveClient(
                "target@example.com",
                "대상회원",
                "대상",
                "010-3333-4444");
        Client viewer = saveClient(
                "viewer@example.com",
                "조회회원",
                "조회",
                "010-4444-5555");
        Category category = saveCategory();
        saveItem(category, client, "판매중 상품", TradeStatus.ON_SALE, false);
        Item soldItem = saveItem(category, client, "판매완료 상품", TradeStatus.SOLD_OUT, false);
        Item deletedReviewItem = saveItem(category, client, "삭제 리뷰 상품", TradeStatus.SOLD_OUT, false);
        saveItem(category, client, "임시저장 상품", TradeStatus.ON_SALE, true);
        followRepository.save(new Follow(viewer, client));
        reviewRepository.save(Review.builder()
                .item(soldItem)
                .reviewer(viewer)
                .reviewee(client)
                .rating(4)
                .content("좋습니다")
                .build());
        Review deletedReview = reviewRepository.save(Review.builder()
                .item(deletedReviewItem)
                .reviewer(viewer)
                .reviewee(client)
                .rating(2)
                .content("삭제된 리뷰")
                .build());
        reviewRepository.delete(deletedReview);
        reviewRepository.flush();
        String accessToken = jwtTokenProvider.createAccessToken(viewer);

        mockMvc.perform(get("/api/clients/{clientId}", client.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.clientId").value(client.getId()))
                .andExpect(jsonPath("$.data.nickname").value("대상회원"))
                .andExpect(jsonPath("$.data.averageRating").value(4.0))
                .andExpect(jsonPath("$.data.reviewCount").value(1))
                .andExpect(jsonPath("$.data.followerCount").value(1))
                .andExpect(jsonPath("$.data.isFollowing").value(true))
                .andExpect(jsonPath("$.data.sellingItemCount").value(1))
                .andExpect(jsonPath("$.data.soldItemCount").value(2));
    }

    @DisplayName("존재하지 않는 회원 공개 프로필 조회 시 404를 반환한다")
    @Test
    void 존재하지_않는_회원_공개_프로필_조회_시_404를_반환한다() throws Exception {
        mockMvc.perform(get("/api/clients/{clientId}", 9999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    @DisplayName("삭제된 회원 공개 프로필 조회 시 404를 반환한다")
    @Test
    void 삭제된_회원_공개_프로필_조회_시_404를_반환한다() throws Exception {
        Client client = saveClient(
                "deleted@example.com",
                "삭제회원",
                "삭제",
                "010-5555-6666");
        Long clientId = client.getId();
        clientRepository.delete(client);
        clientRepository.flush();

        mockMvc.perform(get("/api/clients/{clientId}", clientId))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("CLIENT_NOT_FOUND"));
    }

    @DisplayName("인증된 회원은 내 정보를 부분 수정할 수 있다")
    @Test
    void 인증된_회원은_내_정보를_부분_수정할_수_있다() throws Exception {
        Client client = saveClient(
                "update@example.com",
                "beforeNickname",
                "홍길동",
                "010-1234-5678");
        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(patch("/api/clients/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "afterNickname",
                                  "profileImageUrl": "http://localhost:9000/me.png"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.clientId").value(client.getId()))
                .andExpect(jsonPath("$.data.nickname").value("afterNickname"))
                .andExpect(jsonPath("$.data.name").value("홍길동"))
                .andExpect(jsonPath("$.data.phone").value("010-1234-5678"))
                .andExpect(jsonPath("$.data.profileImageUrl").value("http://localhost:9000/me.png"));

        Client updatedClient = clientRepository.findById(client.getId()).orElseThrow();
        assertThat(updatedClient.getNickname()).isEqualTo("afterNickname");
        assertThat(updatedClient.getProfileImageUrl()).isEqualTo("http://localhost:9000/me.png");
        assertThat(updatedClient.getName()).isEqualTo("홍길동");
        assertThat(updatedClient.getPhone()).isEqualTo("010-1234-5678");
    }

    @DisplayName("내 정보 수정 시 전달하지 않은 필드는 기존 값을 유지한다")
    @Test
    void 내_정보_수정_시_전달하지_않은_필드는_기존_값을_유지한다() throws Exception {
        Client client = saveClient(
                "keep@example.com",
                "keepNickname",
                "기존이름",
                "010-2222-3333");

        String accessToken = jwtTokenProvider.createAccessToken(client);

        mockMvc.perform(patch("/api/clients/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "새이름"
                                }
                                """))
                .andExpect(status().isOk())

                .andExpect(jsonPath("$.data.nickname").value("keepNickname"))
                .andExpect(jsonPath("$.data.name").value("새이름"))
                .andExpect(jsonPath("$.data.phone").value("010-2222-3333"))
                .andExpect(jsonPath("$.data.profileImageUrl").value(Client.defaultProfileImageUrl()));

    }

    @DisplayName("내 정보 수정 시 프로필 이미지 URL 형식이 잘못되면 400을 반환한다")
    @Test
    void 내_정보_수정_시_프로필_이미지_URL_형식이_잘못되면_400을_반환한다() throws Exception {
        Client client = saveClient(
                "invalid-url@example.com",
                "nickname",
                "홍길동",
                "010-1234-5678");
        String accessToken = jwtTokenProvider.createAccessToken(client);
        mockMvc.perform(patch("/api/clients/me")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "profileImageUrl": "not-a-url"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

    }

    @DisplayName("토큰 없이 내 정보 수정을 요청하면 401을 반환한다")
    @Test
    void 토큰_없이_내_정보_수정을_요청하면_401을_반환한다() throws Exception {
        mockMvc.perform(patch("/api/clients/me")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "nickname": "afterNickname"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    private Client saveClient(String email, String nickname, String name, String phone) {
        return clientRepository.save(Client.create(
                email,
                passwordEncoder.encode("password123!"),
                nickname,
                name,
                phone));
    }

    private Category saveCategory() {
        return categoryRepository.save(Category.builder()
                .name("디지털/가전-" + System.nanoTime())
                .sortOrder(1)
                .isActive(true)
                .build());
    }

    private Item saveItem(Category category, Client seller, String title, TradeStatus tradeStatus, boolean isDraft) {
        return itemRepository.save(Item.builder()
                .category(category)
                .seller(seller)
                .tradeType(TradeType.DIRECT)
                .title(title)
                .description("설명")
                .initialPrice(1000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(tradeStatus)
                .isDraft(isDraft)
                .build());
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

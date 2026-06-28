package com.example.cabbagemarket10.domain.inquiry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.category.repository.CategoryRepository;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import com.example.cabbagemarket10.domain.inquiry.repository.InquiryLogRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import java.sql.Timestamp;
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
class InquiryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private InquiryLogRepository inquiryLogRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUp() {
        jdbcTemplate.update("delete from inquiry_log");
        jdbcTemplate.update("delete from item");
        jdbcTemplate.update("delete from category");
        jdbcTemplate.update("delete from client");
    }

    @DisplayName("인증 없이 상품 문의 목록을 기본 페이징으로 조회할 수 있고 답변 행은 제외된다")
    @Test
    void 인증_없이_상품_문의_목록을_기본_페이징으로_조회할_수_있고_답변_행은_제외된다() throws Exception {
        Client author = saveClient("list-author@example.com", "목록작성자", "홍길동");
        Client seller = saveClient("list-seller@example.com", "목록판매자", "김판매");
        Item item = saveItem(seller);
        InquiryLog olderInquiry = saveInquiry(
                item,
                author,
                null,
                "첫 번째 문의입니다.",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        InquiryLog newerInquiry = saveInquiry(
                item,
                author,
                null,
                "두 번째 문의입니다.",
                LocalDateTime.of(2026, 6, 26, 11, 0));
        saveInquiry(
                item,
                seller,
                olderInquiry,
                "첫 번째 문의에 대한 답변입니다.",
                LocalDateTime.of(2026, 6, 26, 12, 0));

        mockMvc.perform(get("/api/items/{itemId}/inquiries", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.page").value(0))
                .andExpect(jsonPath("$.data.size").value(20))
                .andExpect(jsonPath("$.data.totalElements").value(2))
                .andExpect(jsonPath("$.data.totalPages").value(1))
                .andExpect(jsonPath("$.data.itemList.length()").value(2))
                .andExpect(jsonPath("$.data.itemList[0].inquiryId").value(newerInquiry.getId()))
                .andExpect(jsonPath("$.data.itemList[0].authorName").value("홍길동"))
                .andExpect(jsonPath("$.data.itemList[0].contents").value("두 번째 문의입니다."))
                .andExpect(jsonPath("$.data.itemList[0].date").exists())
                .andExpect(jsonPath("$.data.itemList[1].inquiryId").value(olderInquiry.getId()))
                .andExpect(jsonPath("$.data.itemList[1].contents").value("첫 번째 문의입니다."));
    }

    @DisplayName("상품 문의 목록 조회 시 요청한 page와 size 메타데이터를 반환한다")
    @Test
    void 상품_문의_목록_조회_시_요청한_page와_size_메타데이터를_반환한다() throws Exception {
        Client author = saveClient("paged-list-author@example.com", "페이징작성자", "이작성");
        Client seller = saveClient("paged-list-seller@example.com", "페이징판매자", "박판매");
        Item item = saveItem(seller);
        saveInquiry(item, author, null, "첫 번째 문의입니다.", LocalDateTime.of(2026, 6, 26, 10, 0));
        saveInquiry(item, author, null, "두 번째 문의입니다.", LocalDateTime.of(2026, 6, 26, 11, 0));
        saveInquiry(item, author, null, "세 번째 문의입니다.", LocalDateTime.of(2026, 6, 26, 12, 0));

        mockMvc.perform(get("/api/items/{itemId}/inquiries", item.getId())
                        .param("page", "1")
                        .param("size", "2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.page").value(1))
                .andExpect(jsonPath("$.data.size").value(2))
                .andExpect(jsonPath("$.data.totalElements").value(3))
                .andExpect(jsonPath("$.data.totalPages").value(2))
                .andExpect(jsonPath("$.data.itemList.length()").value(1))
                .andExpect(jsonPath("$.data.itemList[0].contents").value("첫 번째 문의입니다."));
    }

    @DisplayName("상품 문의 목록 조회 시 page 또는 size가 잘못되면 VALIDATION_ERROR 400 응답을 반환한다")
    @Test
    void 상품_문의_목록_조회_시_page_또는_size가_잘못되면_VALIDATION_ERROR_400_응답을_반환한다() throws Exception {
        Client seller = saveClient("invalid-list-seller@example.com", "목록검증판매자", "최판매");
        Item item = saveItem(seller);

        mockMvc.perform(get("/api/items/{itemId}/inquiries", item.getId())
                        .param("page", "-1"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));

        mockMvc.perform(get("/api/items/{itemId}/inquiries", item.getId())
                        .param("size", "0"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("인증된 회원은 상품 문의를 생성할 수 있다")
    @Test
    void 인증된_회원은_상품_문의를_생성할_수_있다() throws Exception {
        Client author = saveClient("author@example.com", "문의작성자", "홍길동");
        Client seller = saveClient("seller@example.com", "판매자", "김판매");
        Item item = saveItem(seller);
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(post("/api/items/{itemId}/inquiries", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("상품 문의", "거래 가능한가요?")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.id").exists())
                .andExpect(jsonPath("$.data.authorName").value("홍길동"))
                .andExpect(jsonPath("$.data.contents").value("거래 가능한가요?"))
                .andExpect(jsonPath("$.data.date").exists());

        String savedTitle = jdbcTemplate.queryForObject(
                "select title from inquiry_log where item_id = ? and author_id = ?",
                String.class,
                item.getId(),
                author.getId());
        String savedDescription = jdbcTemplate.queryForObject(
                "select description from inquiry_log where item_id = ? and author_id = ?",
                String.class,
                item.getId(),
                author.getId());
        String savedStatus = jdbcTemplate.queryForObject(
                "select status from inquiry_log where item_id = ? and author_id = ?",
                String.class,
                item.getId(),
                author.getId());
        assertThat(savedTitle).isEqualTo("상품 문의");
        assertThat(savedDescription).isEqualTo("거래 가능한가요?");
        assertThat(savedStatus).isEqualTo("QUESTION");
    }

    @DisplayName("문의 내용은 2000자까지 생성할 수 있다")
    @Test
    void 문의_내용은_2000자까지_생성할_수_있다() throws Exception {
        Client author = saveClient("boundary-author@example.com", "경계작성자", "이경계");
        Client seller = saveClient("boundary-seller@example.com", "경계판매자", "박판매");
        Item item = saveItem(seller);
        String accessToken = jwtTokenProvider.createAccessToken(author);
        String contents = "가".repeat(2000);

        mockMvc.perform(post("/api/items/{itemId}/inquiries", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("내용 길이 문의", contents)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.contents").value(contents));
    }

    @DisplayName("문의 제목이 없으면 400 응답을 반환한다")
    @Test
    void 문의_제목이_없으면_400_응답을_반환한다() throws Exception {
        Client author = saveClient("missing-title-author@example.com", "제목누락작성자", "최작성");
        Client seller = saveClient("missing-title-seller@example.com", "제목누락판매자", "정판매");
        Item item = saveItem(seller);
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(post("/api/items/{itemId}/inquiries", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contents": "거래 가능한가요?"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("문의 제목은 200자까지 생성할 수 있다")
    @Test
    void 문의_제목은_200자까지_생성할_수_있다() throws Exception {
        Client author = saveClient("boundary-title-author@example.com", "제목경계작성자", "길작성");
        Client seller = saveClient("boundary-title-seller@example.com", "제목경계판매자", "배판매");
        Item item = saveItem(seller);
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(post("/api/items/{itemId}/inquiries", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("가".repeat(200), "거래 가능한가요?")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201));
    }

    @DisplayName("문의 제목이 200자를 초과하면 400 응답을 반환한다")
    @Test
    void 문의_제목이_200자를_초과하면_400_응답을_반환한다() throws Exception {
        Client author = saveClient("too-long-title-author@example.com", "제목길이작성자", "문작성");
        Client seller = saveClient("too-long-title-seller@example.com", "제목길이판매자", "남판매");
        Item item = saveItem(seller);
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(post("/api/items/{itemId}/inquiries", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("가".repeat(201), "거래 가능한가요?")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("문의 내용이 공백이면 400 응답을 반환한다")
    @Test
    void 문의_내용이_공백이면_400_응답을_반환한다() throws Exception {
        Client author = saveClient("blank-contents-author@example.com", "공백작성자", "오작성");
        Client seller = saveClient("blank-contents-seller@example.com", "공백판매자", "한판매");
        Item item = saveItem(seller);
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(post("/api/items/{itemId}/inquiries", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("상품 문의", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("문의 내용이 2000자를 초과하면 400 응답을 반환한다")
    @Test
    void 문의_내용이_2000자를_초과하면_400_응답을_반환한다() throws Exception {
        Client author = saveClient("too-long-author@example.com", "길이작성자", "임작성");
        Client seller = saveClient("too-long-seller@example.com", "길이판매자", "윤판매");
        Item item = saveItem(seller);
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(post("/api/items/{itemId}/inquiries", item.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("상품 문의", "가".repeat(2001))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("토큰이 없으면 문의를 생성할 수 없다")
    @Test
    void 토큰이_없으면_문의를_생성할_수_없다() throws Exception {
        Client seller = saveClient("unauthorized-seller@example.com", "미인증판매자", "서판매");
        Item item = saveItem(seller);

        mockMvc.perform(post("/api/items/{itemId}/inquiries", item.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("상품 문의", "거래 가능한가요?")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("존재하지 않는 상품이면 ITEM_NOT_FOUND 404 응답을 반환한다")
    @Test
    void 존재하지_않는_상품이면_ITEM_NOT_FOUND_404_응답을_반환한다() throws Exception {
        Client author = saveClient("not-found-author@example.com", "미존재작성자", "강작성");
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(post("/api/items/{itemId}/inquiries", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("상품 문의", "거래 가능한가요?")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("ITEM_NOT_FOUND"));
    }

    private String json(String title, String contents) {
        return """
                {
                  "title": "%s",
                  "contents": "%s"
                }
                """.formatted(title, contents);
    }

    private Client saveClient(String email, String nickname, String name) {
        return clientRepository.save(Client.create(
                email,
                passwordEncoder.encode("password123!"),
                nickname,
                name,
                "010-1234-5678"));
    }

    private Item saveItem(Client seller) {
        Category category = categoryRepository.save(Category.builder()
                .name("디지털/가전")
                .sortOrder(1)
                .isActive(true)
                .build());

        return itemRepository.save(Item.builder()
                .category(category)
                .seller(seller)
                .tradeType(TradeType.DIRECT)
                .title("중고 노트북")
                .description("상태 좋은 노트북입니다.")
                .initialPrice(100_000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build());
    }

    private InquiryLog saveInquiry(
            Item item,
            Client author,
            InquiryLog targetInquiry,
            String contents,
            LocalDateTime createdAt
    ) {
        InquiryLog savedInquiry = inquiryLogRepository.save(InquiryLog.builder()
                .item(item)
                .author(author)
                .targetInquiry(targetInquiry)
                .title("상품 문의")
                .description(contents)
                .status(targetInquiry == null ? "QUESTION" : "ANSWER")
                .build());

        jdbcTemplate.update(
                "update inquiry_log set created_at = ?, updated_at = ? where id = ?",
                Timestamp.valueOf(createdAt),
                Timestamp.valueOf(createdAt),
                savedInquiry.getId());
        return savedInquiry;
    }
}

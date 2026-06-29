package com.example.cabbagemarket10.domain.inquiry.controller;

import static org.assertj.core.api.Assertions.assertThat;
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
class InquiryAnswerControllerTest {

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

    @DisplayName("상품 판매자는 문의 답변을 등록할 수 있다")
    @Test
    void 상품_판매자는_문의_답변을_등록할_수_있다() throws Exception {
        Client author = saveClient("answer-author@example.com", "답변문의자", "홍길동");
        Client seller = saveClient("answer-seller@example.com", "답변판매자", "김판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        String accessToken = jwtTokenProvider.createAccessToken(seller);

        mockMvc.perform(post("/api/inquiries/{inquiryId}/answer", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("답변입니다", "거래 가능합니다.")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.inquiryId").value(inquiry.getId()))
                .andExpect(jsonPath("$.data.answerData.id").exists())
                .andExpect(jsonPath("$.data.answerData.authorName").value("김판매"))
                .andExpect(jsonPath("$.data.answerData.contents").value("거래 가능합니다."))
                .andExpect(jsonPath("$.data.answerData.date").exists());

        Long savedTargetInquiryId = jdbcTemplate.queryForObject(
                "select target_inquiry_id from inquiry_log where author_id = ? and status = 'ANSWER'",
                Long.class,
                seller.getId());
        String savedTitle = jdbcTemplate.queryForObject(
                "select title from inquiry_log where author_id = ? and status = 'ANSWER'",
                String.class,
                seller.getId());
        String savedDescription = jdbcTemplate.queryForObject(
                "select description from inquiry_log where author_id = ? and status = 'ANSWER'",
                String.class,
                seller.getId());
        assertThat(savedTargetInquiryId).isEqualTo(inquiry.getId());
        assertThat(savedTitle).isEqualTo("답변입니다");
        assertThat(savedDescription).isEqualTo("거래 가능합니다.");
    }

    @DisplayName("문의 답변 제목이 없으면 400 응답을 반환한다")
    @Test
    void 문의_답변_제목이_없으면_400_응답을_반환한다() throws Exception {
        Client author = saveClient("missing-answer-title-author@example.com", "답변제목누락문의자", "홍길동");
        Client seller = saveClient("missing-answer-title-seller@example.com", "답변제목누락판매자", "김판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        String accessToken = jwtTokenProvider.createAccessToken(seller);

        mockMvc.perform(post("/api/inquiries/{inquiryId}/answer", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contents": "거래 가능합니다."
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("문의 답변 내용이 없으면 400 응답을 반환한다")
    @Test
    void 문의_답변_내용이_없으면_400_응답을_반환한다() throws Exception {
        Client author = saveClient("missing-answer-contents-author@example.com", "답변내용누락문의자", "홍길동");
        Client seller = saveClient("missing-answer-contents-seller@example.com", "답변내용누락판매자", "김판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        String accessToken = jwtTokenProvider.createAccessToken(seller);

        mockMvc.perform(post("/api/inquiries/{inquiryId}/answer", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "title": "답변입니다"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("토큰이 없으면 문의 답변을 등록할 수 없다")
    @Test
    void 토큰이_없으면_문의_답변을_등록할_수_없다() throws Exception {
        Client author = saveClient("unauthorized-answer-author@example.com", "미인증답변문의자", "홍길동");
        Client seller = saveClient("unauthorized-answer-seller@example.com", "미인증답변판매자", "김판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));

        mockMvc.perform(post("/api/inquiries/{inquiryId}/answer", inquiry.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("답변입니다", "거래 가능합니다.")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("상품 판매자가 아니면 문의 답변 등록 시 403 응답을 반환한다")
    @Test
    void 상품_판매자가_아니면_문의_답변_등록_시_403_응답을_반환한다() throws Exception {
        Client author = saveClient("forbidden-answer-author@example.com", "권한답변문의자", "홍길동");
        Client seller = saveClient("forbidden-answer-seller@example.com", "권한답변판매자", "김판매");
        Client otherClient = saveClient("forbidden-answer-other@example.com", "권한없는회원", "이회원");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        String accessToken = jwtTokenProvider.createAccessToken(otherClient);

        mockMvc.perform(post("/api/inquiries/{inquiryId}/answer", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("답변입니다", "거래 가능합니다.")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @DisplayName("이미 답변이 존재하면 문의 답변 등록 시 409 응답을 반환한다")
    @Test
    void 이미_답변이_존재하면_문의_답변_등록_시_409_응답을_반환한다() throws Exception {
        Client author = saveClient("conflict-answer-author@example.com", "중복답변문의자", "홍길동");
        Client seller = saveClient("conflict-answer-seller@example.com", "중복답변판매자", "김판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        saveInquiry(
                item,
                seller,
                inquiry,
                "이미 답변했습니다.",
                LocalDateTime.of(2026, 6, 26, 11, 0));
        String accessToken = jwtTokenProvider.createAccessToken(seller);

        mockMvc.perform(post("/api/inquiries/{inquiryId}/answer", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("답변입니다", "거래 가능합니다.")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.code").value("ANSWER_ALREADY_EXISTS"));
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

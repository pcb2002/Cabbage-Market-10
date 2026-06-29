package com.example.cabbagemarket10.domain.inquiry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
class InquiryUpdateControllerTest {

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

    @DisplayName("문의 작성자는 문의를 수정할 수 있다")
    @Test
    void 문의_작성자는_문의를_수정할_수_있다() throws Exception {
        Client author = saveClient("update-author@example.com", "수정작성자", "홍길동");
        Client seller = saveClient("update-seller@example.com", "수정판매자", "김판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(put("/api/inquiries/{inquiryId}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("수정된 문의", "가격 조정 가능한가요?")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.id").value(inquiry.getId()))
                .andExpect(jsonPath("$.data.authorName").value("홍길동"))
                .andExpect(jsonPath("$.data.contents").value("가격 조정 가능한가요?"))
                .andExpect(jsonPath("$.data.date").exists());

        String savedTitle = jdbcTemplate.queryForObject(
                "select title from inquiry_log where id = ?",
                String.class,
                inquiry.getId());
        String savedDescription = jdbcTemplate.queryForObject(
                "select description from inquiry_log where id = ?",
                String.class,
                inquiry.getId());
        assertThat(savedTitle).isEqualTo("수정된 문의");
        assertThat(savedDescription).isEqualTo("가격 조정 가능한가요?");
    }

    @DisplayName("문의 수정 요청에 제목이 없으면 기존 제목을 유지한다")
    @Test
    void 문의_수정_요청에_제목이_없으면_기존_제목을_유지한다() throws Exception {
        Client author = saveClient("update-content-author@example.com", "내용수정작성자", "이작성");
        Client seller = saveClient("update-content-seller@example.com", "내용수정판매자", "박판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(put("/api/inquiries/{inquiryId}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "contents": "구성품 포함인가요?"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.contents").value("구성품 포함인가요?"));

        String savedTitle = jdbcTemplate.queryForObject(
                "select title from inquiry_log where id = ?",
                String.class,
                inquiry.getId());
        assertThat(savedTitle).isEqualTo("상품 문의");
    }

    @DisplayName("문의 수정 시 내용이 공백이면 400 응답을 반환한다")
    @Test
    void 문의_수정_시_내용이_공백이면_400_응답을_반환한다() throws Exception {
        Client author = saveClient("update-blank-author@example.com", "수정검증작성자", "최작성");
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(put("/api/inquiries/{inquiryId}", 1L)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("수정된 문의", "   ")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @DisplayName("토큰이 없으면 문의를 수정할 수 없다")
    @Test
    void 토큰이_없으면_문의를_수정할_수_없다() throws Exception {
        mockMvc.perform(put("/api/inquiries/{inquiryId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("수정된 문의", "가격 조정 가능한가요?")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("문의 작성자가 아니면 문의 수정 시 403 응답을 반환한다")
    @Test
    void 문의_작성자가_아니면_문의_수정_시_403_응답을_반환한다() throws Exception {
        Client author = saveClient("forbidden-author@example.com", "권한작성자", "정작성");
        Client other = saveClient("forbidden-other@example.com", "다른사용자", "한사용");
        Client seller = saveClient("forbidden-seller@example.com", "권한판매자", "윤판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        String accessToken = jwtTokenProvider.createAccessToken(other);

        mockMvc.perform(put("/api/inquiries/{inquiryId}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("수정된 문의", "가격 조정 가능한가요?")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @DisplayName("존재하지 않는 문의를 수정하면 INQUIRY_NOT_FOUND 404 응답을 반환한다")
    @Test
    void 존재하지_않는_문의를_수정하면_INQUIRY_NOT_FOUND_404_응답을_반환한다() throws Exception {
        Client author = saveClient("update-not-found-author@example.com", "미존재수정작성자", "남작성");
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(put("/api/inquiries/{inquiryId}", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("수정된 문의", "가격 조정 가능한가요?")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("INQUIRY_NOT_FOUND"));
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

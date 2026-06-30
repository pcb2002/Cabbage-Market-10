package com.example.cabbagemarket10.domain.inquiry.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
class InquiryDeleteControllerTest {

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
        jdbcTemplate.update("delete from review");
        jdbcTemplate.update("delete from inquiry_log");
        jdbcTemplate.update("delete from item");
        jdbcTemplate.update("delete from category");
        jdbcTemplate.update("delete from client");
    }

    @DisplayName("문의 작성자는 문의를 소프트 삭제할 수 있다")
    @Test
    void 문의_작성자는_문의를_소프트_삭제할_수_있다() throws Exception {
        Client author = saveClient("delete-author@example.com", "삭제작성자", "홍길동");
        Client seller = saveClient("delete-seller@example.com", "삭제판매자", "김판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(delete("/api/inquiries/{inquiryId}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data").value(nullValue()));

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "select is_deleted from inquiry_log where id = ?",
                Boolean.class,
                inquiry.getId());
        assertThat(isDeleted).isTrue();

        mockMvc.perform(get("/api/items/{itemId}/inquiries", item.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(0))
                .andExpect(jsonPath("$.data.itemList.length()").value(0));
    }

    @DisplayName("토큰이 없으면 문의를 삭제할 수 없다")
    @Test
    void 토큰이_없으면_문의를_삭제할_수_없다() throws Exception {
        mockMvc.perform(delete("/api/inquiries/{inquiryId}", 1L)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.code").value("UNAUTHORIZED"));
    }

    @DisplayName("문의 작성자가 아니면 문의 삭제 시 403 응답을 반환한다")
    @Test
    void 문의_작성자가_아니면_문의_삭제_시_403_응답을_반환한다() throws Exception {
        Client author = saveClient("delete-forbidden-author@example.com", "권한작성자", "정작성");
        Client other = saveClient("delete-forbidden-other@example.com", "다른사용자", "한사용");
        Client seller = saveClient("delete-forbidden-seller@example.com", "권한판매자", "윤판매");
        Item item = saveItem(seller);
        InquiryLog inquiry = saveInquiry(
                item,
                author,
                null,
                "거래 가능한가요?",
                LocalDateTime.of(2026, 6, 26, 10, 0));
        String accessToken = jwtTokenProvider.createAccessToken(other);

        mockMvc.perform(delete("/api/inquiries/{inquiryId}", inquiry.getId())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));

        Boolean isDeleted = jdbcTemplate.queryForObject(
                "select is_deleted from inquiry_log where id = ?",
                Boolean.class,
                inquiry.getId());
        assertThat(isDeleted).isFalse();
    }

    @DisplayName("존재하지 않는 문의를 삭제하면 INQUIRY_NOT_FOUND 404 응답을 반환한다")
    @Test
    void 존재하지_않는_문의를_삭제하면_INQUIRY_NOT_FOUND_404_응답을_반환한다() throws Exception {
        Client author = saveClient("delete-not-found-author@example.com", "미존재삭제작성자", "남작성");
        String accessToken = jwtTokenProvider.createAccessToken(author);

        mockMvc.perform(delete("/api/inquiries/{inquiryId}", Long.MAX_VALUE)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.code").value("INQUIRY_NOT_FOUND"));
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

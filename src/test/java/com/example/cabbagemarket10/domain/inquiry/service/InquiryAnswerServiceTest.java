package com.example.cabbagemarket10.domain.inquiry.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryAnswerCreateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryAnswerCreateResponse;
import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import com.example.cabbagemarket10.domain.inquiry.repository.InquiryLogRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InquiryAnswerServiceTest {

    @Mock
    private InquiryLogRepository inquiryLogRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private InquiryService inquiryService;

    @DisplayName("상품 판매자는 문의 답변을 등록할 수 있다")
    @Test
    void 상품_판매자는_문의_답변을_등록할_수_있다() {
        Client seller = client("seller@example.com", "판매자", "김판매");
        ReflectionTestUtils.setField(seller, "id", 2L);
        Client author = client("author@example.com", "문의작성자", "홍길동");
        Item item = item(seller);
        InquiryLog inquiry = inquiry(item, author, "상품 문의", "거래 가능한가요?");
        ReflectionTestUtils.setField(inquiry, "id", 10L);
        InquiryAnswerCreateRequest request = new InquiryAnswerCreateRequest("답변입니다", "거래 가능합니다.");
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 26, 13, 0);

        given(inquiryLogRepository.findQuestionByIdWithItemSeller(10L)).willReturn(Optional.of(inquiry));
        given(inquiryLogRepository.existsByTargetInquiryId(10L)).willReturn(false);
        given(inquiryLogRepository.save(any(InquiryLog.class))).willAnswer(invocation -> {
            InquiryLog answer = invocation.getArgument(0);
            ReflectionTestUtils.setField(answer, "id", 20L);
            ReflectionTestUtils.setField(answer, "createdAt", createdAt);
            return answer;
        });

        InquiryAnswerCreateResponse response = inquiryService.createAnswer(10L, 2L, request);

        ArgumentCaptor<InquiryLog> captor = ArgumentCaptor.forClass(InquiryLog.class);
        verify(inquiryLogRepository).save(captor.capture());
        InquiryLog savedAnswer = captor.getValue();
        assertThat(savedAnswer.getItem()).isSameAs(item);
        assertThat(savedAnswer.getAuthor()).isSameAs(seller);
        assertThat(savedAnswer.getTargetInquiry()).isSameAs(inquiry);
        assertThat(savedAnswer.getTitle()).isEqualTo("답변입니다");
        assertThat(savedAnswer.getDescription()).isEqualTo("거래 가능합니다.");
        assertThat(savedAnswer.getStatus()).isEqualTo("ANSWER");

        assertThat(response.inquiryId()).isEqualTo(10L);
        assertThat(response.answerData().id()).isEqualTo(20L);
        assertThat(response.answerData().authorName()).isEqualTo("김판매");
        assertThat(response.answerData().contents()).isEqualTo("거래 가능합니다.");
        assertThat(response.answerData().date()).isEqualTo(createdAt);
    }

    @DisplayName("문의 답변 등록 시 문의가 없으면 INQUIRY_NOT_FOUND 예외가 발생한다")
    @Test
    void 문의_답변_등록_시_문의가_없으면_INQUIRY_NOT_FOUND_예외가_발생한다() {
        InquiryAnswerCreateRequest request = new InquiryAnswerCreateRequest("답변입니다", "거래 가능합니다.");
        given(inquiryLogRepository.findQuestionByIdWithItemSeller(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.createAnswer(10L, 2L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_FOUND));

        verify(inquiryLogRepository, never()).existsByTargetInquiryId(any());
        verify(inquiryLogRepository, never()).save(any());
    }

    @DisplayName("상품 판매자가 아니면 문의 답변 등록 시 FORBIDDEN 예외가 발생한다")
    @Test
    void 상품_판매자가_아니면_문의_답변_등록_시_FORBIDDEN_예외가_발생한다() {
        Client seller = client("seller@example.com", "판매자", "김판매");
        ReflectionTestUtils.setField(seller, "id", 2L);
        Item item = item(seller);
        InquiryLog inquiry = inquiry(item, client("author@example.com", "문의작성자", "홍길동"), "상품 문의", "거래 가능한가요?");
        InquiryAnswerCreateRequest request = new InquiryAnswerCreateRequest("답변입니다", "거래 가능합니다.");

        given(inquiryLogRepository.findQuestionByIdWithItemSeller(10L)).willReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> inquiryService.createAnswer(10L, 3L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        verify(inquiryLogRepository, never()).existsByTargetInquiryId(any());
        verify(inquiryLogRepository, never()).save(any());
    }

    @DisplayName("이미 답변이 존재하면 문의 답변 등록 시 ANSWER_ALREADY_EXISTS 예외가 발생한다")
    @Test
    void 이미_답변이_존재하면_문의_답변_등록_시_ANSWER_ALREADY_EXISTS_예외가_발생한다() {
        Client seller = client("seller@example.com", "판매자", "김판매");
        ReflectionTestUtils.setField(seller, "id", 2L);
        Item item = item(seller);
        InquiryLog inquiry = inquiry(item, client("author@example.com", "문의작성자", "홍길동"), "상품 문의", "거래 가능한가요?");
        InquiryAnswerCreateRequest request = new InquiryAnswerCreateRequest("답변입니다", "거래 가능합니다.");

        given(inquiryLogRepository.findQuestionByIdWithItemSeller(10L)).willReturn(Optional.of(inquiry));
        given(inquiryLogRepository.existsByTargetInquiryId(10L)).willReturn(true);

        assertThatThrownBy(() -> inquiryService.createAnswer(10L, 2L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ANSWER_ALREADY_EXISTS));

        verify(inquiryLogRepository, never()).save(any());
    }

    @DisplayName("중복 답변 저장 무결성 위반 시 ANSWER_ALREADY_EXISTS 예외가 발생한다")
    @Test
    void 중복_답변_저장_무결성_위반_시_ANSWER_ALREADY_EXISTS_예외가_발생한다() {
        Client seller = client("seller@example.com", "판매자", "김판매");
        ReflectionTestUtils.setField(seller, "id", 2L);
        Item item = item(seller);
        InquiryLog inquiry = inquiry(item, client("author@example.com", "문의작성자", "홍길동"), "상품 문의", "거래 가능한가요?");
        InquiryAnswerCreateRequest request = new InquiryAnswerCreateRequest("답변입니다", "거래 가능합니다.");

        given(inquiryLogRepository.findQuestionByIdWithItemSeller(10L)).willReturn(Optional.of(inquiry));
        given(inquiryLogRepository.existsByTargetInquiryId(10L)).willReturn(false);
        given(inquiryLogRepository.save(any(InquiryLog.class)))
                .willThrow(new DataIntegrityViolationException("duplicate answer"));

        assertThatThrownBy(() -> inquiryService.createAnswer(10L, 2L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ANSWER_ALREADY_EXISTS));
    }

    private Item item(Client seller) {
        return Item.builder()
                .category(Category.builder()
                        .name("디지털/가전")
                        .sortOrder(1)
                        .isActive(true)
                        .build())
                .seller(seller)
                .tradeType(TradeType.DIRECT)
                .title("중고 노트북")
                .description("상태 좋은 노트북입니다.")
                .initialPrice(100_000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(TradeStatus.ON_SALE)
                .isDraft(false)
                .build();
    }

    private InquiryLog inquiry(Item item, Client author, String title, String description) {
        return InquiryLog.builder()
                .item(item)
                .author(author)
                .title(title)
                .description(description)
                .status("QUESTION")
                .build();
    }

    private Client client(String email, String nickname, String name) {
        return Client.create(
                email,
                "encodedPassword",
                nickname,
                name,
                "010-1234-5678");
    }
}

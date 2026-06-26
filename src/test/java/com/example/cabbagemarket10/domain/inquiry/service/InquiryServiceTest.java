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
import com.example.cabbagemarket10.domain.inquiry.dto.request.InquiryCreateRequest;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryCreateResponse;
import com.example.cabbagemarket10.domain.inquiry.dto.response.InquiryListResponse;
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
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InquiryServiceTest {

    @Mock
    private InquiryLogRepository inquiryLogRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private InquiryService inquiryService;

    @DisplayName("상품이 존재하면 문의 목록을 페이징 응답하고 Pageable로 조회한다")
    @Test
    void 상품이_존재하면_문의_목록을_페이징_응답하고_Pageable로_조회한다() {
        Item item = item();
        Client author = client("author@example.com", "문의작성자", "홍길동");
        InquiryLog inquiry = inquiry(item, author, "상품 문의", "거래 가능한가요?");
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 26, 12, 0);
        ReflectionTestUtils.setField(inquiry, "id", 10L);
        ReflectionTestUtils.setField(inquiry, "createdAt", createdAt);
        Pageable pageable = PageRequest.of(1, 2);

        given(itemRepository.existsById(1L)).willReturn(true);
        given(inquiryLogRepository.findRootInquiriesByItemIdWithAuthor(1L, pageable))
                .willReturn(new PageImpl<>(List.of(inquiry), pageable, 3));

        InquiryListResponse response = inquiryService.getInquiries(1L, 1, 2);

        verify(inquiryLogRepository).findRootInquiriesByItemIdWithAuthor(1L, pageable);
        assertThat(response.itemList()).hasSize(1);
        assertThat(response.itemList().get(0).enquiryID()).isEqualTo(10L);
        assertThat(response.itemList().get(0).authorName()).isEqualTo("홍길동");
        assertThat(response.itemList().get(0).contents()).isEqualTo("거래 가능한가요?");
        assertThat(response.itemList().get(0).date()).isEqualTo(createdAt);
        assertThat(response.page()).isEqualTo(1);
        assertThat(response.size()).isEqualTo(2);
        assertThat(response.totalElements()).isEqualTo(3);
        assertThat(response.totalPages()).isEqualTo(2);
    }

    @DisplayName("문의 목록 조회 시 상품이 없으면 ITEM_NOT_FOUND 예외가 발생하고 문의를 조회하지 않는다")
    @Test
    void 문의_목록_조회_시_상품이_없으면_ITEM_NOT_FOUND_예외가_발생하고_문의를_조회하지_않는다() {
        given(itemRepository.existsById(1L)).willReturn(false);

        assertThatThrownBy(() -> inquiryService.getInquiries(1L, 0, 20))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND));

        verify(inquiryLogRepository, never()).findRootInquiriesByItemIdWithAuthor(any(), any());
    }

    @DisplayName("문의 생성 성공 시 contents를 description으로 저장하고 응답한다")
    @Test
    void 문의_생성_성공_시_contents를_description으로_저장하고_응답한다() {
        Item item = item();
        Client author = client("author@example.com", "문의작성자", "홍길동");
        InquiryCreateRequest request = new InquiryCreateRequest("상품 문의", "거래 가능한가요?");
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 26, 12, 0);

        given(itemRepository.findById(1L)).willReturn(Optional.of(item));
        given(clientRepository.findById(2L)).willReturn(Optional.of(author));
        given(inquiryLogRepository.save(any(InquiryLog.class))).willAnswer(invocation -> {
            InquiryLog inquiryLog = invocation.getArgument(0);
            ReflectionTestUtils.setField(inquiryLog, "id", 10L);
            ReflectionTestUtils.setField(inquiryLog, "createdAt", createdAt);
            return inquiryLog;
        });

        InquiryCreateResponse response = inquiryService.createInquiry(1L, 2L, request);

        ArgumentCaptor<InquiryLog> captor = ArgumentCaptor.forClass(InquiryLog.class);
        verify(inquiryLogRepository).save(captor.capture());
        InquiryLog savedInquiry = captor.getValue();
        assertThat(savedInquiry.getItem()).isSameAs(item);
        assertThat(savedInquiry.getAuthor()).isSameAs(author);
        assertThat(savedInquiry.getTitle()).isEqualTo("상품 문의");
        assertThat(savedInquiry.getDescription()).isEqualTo("거래 가능한가요?");
        assertThat(savedInquiry.getStatus()).isEqualTo("QUESTION");

        assertThat(response.id()).isEqualTo(10L);
        assertThat(response.authorName()).isEqualTo("홍길동");
        assertThat(response.contents()).isEqualTo("거래 가능한가요?");
        assertThat(response.date()).isEqualTo(createdAt);
    }

    @DisplayName("문의 생성 시 상품이 없으면 ITEM_NOT_FOUND 예외가 발생한다")
    @Test
    void 문의_생성_시_상품이_없으면_ITEM_NOT_FOUND_예외가_발생한다() {
        InquiryCreateRequest request = new InquiryCreateRequest("상품 문의", "거래 가능한가요?");
        given(itemRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.createInquiry(1L, 2L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.ITEM_NOT_FOUND));

        verify(clientRepository, never()).findById(any());
        verify(inquiryLogRepository, never()).save(any());
    }

    @DisplayName("문의 생성 시 작성자가 없으면 CLIENT_NOT_FOUND 예외가 발생한다")
    @Test
    void 문의_생성_시_작성자가_없으면_CLIENT_NOT_FOUND_예외가_발생한다() {
        InquiryCreateRequest request = new InquiryCreateRequest("상품 문의", "거래 가능한가요?");
        given(itemRepository.findById(1L)).willReturn(Optional.of(item()));
        given(clientRepository.findById(2L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.createInquiry(1L, 2L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));

        verify(inquiryLogRepository, never()).save(any());
    }

    private Item item() {
        return Item.builder()
                .category(Category.builder()
                        .name("디지털/가전")
                        .sortOrder(1)
                        .isActive(true)
                        .build())
                .seller(client("seller@example.com", "판매자", "김판매"))
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

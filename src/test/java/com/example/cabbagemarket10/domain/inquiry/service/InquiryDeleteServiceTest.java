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
import com.example.cabbagemarket10.domain.inquiry.entity.InquiryLog;
import com.example.cabbagemarket10.domain.inquiry.repository.InquiryLogRepository;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class InquiryDeleteServiceTest {

    @Mock
    private InquiryLogRepository inquiryLogRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private ClientRepository clientRepository;

    @InjectMocks
    private InquiryService inquiryService;

    @DisplayName("문의 작성자는 문의를 삭제할 수 있다")
    @Test
    void 문의_작성자는_문의를_삭제할_수_있다() {
        Item item = item();
        Client author = client("author@example.com", "문의작성자", "홍길동");
        ReflectionTestUtils.setField(author, "id", 2L);
        InquiryLog inquiry = inquiry(item, author, "상품 문의", "거래 가능한가요?");

        given(inquiryLogRepository.findRootInquiryByIdWithAuthor(10L)).willReturn(Optional.of(inquiry));

        inquiryService.deleteInquiry(10L, 2L);

        verify(inquiryLogRepository).delete(inquiry);
    }

    @DisplayName("문의 삭제 시 문의가 없으면 INQUIRY_NOT_FOUND 예외가 발생한다")
    @Test
    void 문의_삭제_시_문의가_없으면_INQUIRY_NOT_FOUND_예외가_발생한다() {
        given(inquiryLogRepository.findRootInquiryByIdWithAuthor(10L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> inquiryService.deleteInquiry(10L, 2L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.INQUIRY_NOT_FOUND));

        verify(inquiryLogRepository, never()).delete(any());
    }

    @DisplayName("문의 작성자가 아니면 문의 삭제 시 FORBIDDEN 예외가 발생한다")
    @Test
    void 문의_작성자가_아니면_문의_삭제_시_FORBIDDEN_예외가_발생한다() {
        Item item = item();
        Client author = client("author@example.com", "문의작성자", "홍길동");
        ReflectionTestUtils.setField(author, "id", 2L);
        InquiryLog inquiry = inquiry(item, author, "상품 문의", "거래 가능한가요?");

        given(inquiryLogRepository.findRootInquiryByIdWithAuthor(10L)).willReturn(Optional.of(inquiry));

        assertThatThrownBy(() -> inquiryService.deleteInquiry(10L, 3L))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.FORBIDDEN));

        verify(inquiryLogRepository, never()).delete(any());
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

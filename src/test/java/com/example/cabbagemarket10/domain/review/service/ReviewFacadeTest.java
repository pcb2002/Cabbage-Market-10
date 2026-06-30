package com.example.cabbagemarket10.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.application.facade.ReviewFacade;
import com.example.cabbagemarket10.domain.auction.service.AuctionStatusService;
import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.service.ClientService;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.item.service.ItemService;
import com.example.cabbagemarket10.domain.review.dto.request.ReviewCreateRequest;
import com.example.cabbagemarket10.domain.review.dto.response.ReviewCreateResponse;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewFacadeTest {

    @Mock
    private ItemService itemService;

    @Mock
    private ClientService clientService;

    @Mock
    private AuctionStatusService auctionStatusService;

    @Mock
    private ReviewService reviewService;

    @InjectMocks
    private ReviewFacade reviewFacade;

    @DisplayName("경매 낙찰자는 거래완료 상품에 리뷰를 작성할 수 있다")
    @Test
    void 경매_낙찰자는_거래완료_상품에_리뷰를_작성할_수_있다() {
        Client seller = client(1L, "seller@example.com", "판매자", "김판매");
        Client buyer = client(2L, "buyer@example.com", "구매자", "박구매");
        Item item = item(10L, seller, TradeType.AUCTION, TradeStatus.SOLD_OUT, false);
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋은 거래였습니다.");
        ReviewCreateResponse expected = new ReviewCreateResponse(100L, 10L, 2L, 1L, 5, "좋은 거래였습니다.", LocalDateTime.now());

        given(itemService.getItem(10L)).willReturn(item);
        given(clientService.getClient(2L)).willReturn(buyer);
        given(auctionStatusService.getCurrentBidder(10L)).willReturn(buyer);
        given(reviewService.createReview(item, buyer, request)).willReturn(expected);

        ReviewCreateResponse response = reviewFacade.createReview(10L, 2L, request);

        assertThat(response).isSameAs(expected);
    }

    @DisplayName("직거래 구매자는 거래완료 상품에 리뷰를 작성할 수 있다")
    @Test
    void 직거래_구매자는_거래완료_상품에_리뷰를_작성할_수_있다() {
        Client seller = client(1L, "seller@example.com", "판매자", "김판매");
        Client buyer = client(2L, "buyer@example.com", "구매자", "박구매");
        Item item = item(10L, seller, TradeType.DIRECT, TradeStatus.SOLD_OUT, false);
        item.updateStatus(TradeStatus.SOLD_OUT, buyer);
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋은 직거래였습니다.");
        ReviewCreateResponse expected = new ReviewCreateResponse(100L, 10L, 2L, 1L, 5, "좋은 직거래였습니다.", LocalDateTime.now());

        given(itemService.getItem(10L)).willReturn(item);
        given(clientService.getClient(2L)).willReturn(buyer);
        given(reviewService.createReview(item, buyer, request)).willReturn(expected);

        ReviewCreateResponse response = reviewFacade.createReview(10L, 2L, request);

        assertThat(response).isSameAs(expected);
        verify(auctionStatusService, never()).getCurrentBidder(any());
    }

    @DisplayName("거래완료 상태가 아니면 리뷰를 작성할 수 없다")
    @Test
    void 거래완료_상태가_아니면_리뷰를_작성할_수_없다() {
        Client seller = client(1L, "seller@example.com", "판매자", "김판매");
        Item item = item(10L, seller, TradeType.AUCTION, TradeStatus.ON_SALE, false);

        given(itemService.getItem(10L)).willReturn(item);

        assertThatThrownBy(() -> reviewFacade.createReview(10L, 2L, new ReviewCreateRequest(5, "좋아요")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ITEM_NOT_COMPLETED));

        verify(clientService, never()).getClient(any());
        verify(reviewService, never()).createReview(any(), any(), any());
    }

    @DisplayName("임시저장 상품에는 리뷰를 작성할 수 없다")
    @Test
    void 임시저장_상품에는_리뷰를_작성할_수_없다() {
        Client seller = client(1L, "seller@example.com", "판매자", "김판매");
        Item item = item(10L, seller, TradeType.AUCTION, TradeStatus.SOLD_OUT, true);

        given(itemService.getItem(10L)).willReturn(item);

        assertThatThrownBy(() -> reviewFacade.createReview(10L, 2L, new ReviewCreateRequest(5, "좋아요")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ITEM_NOT_COMPLETED));

        verify(reviewService, never()).createReview(any(), any(), any());
    }

    @DisplayName("판매자는 자신의 상품에 리뷰를 작성할 수 없다")
    @Test
    void 판매자는_자신의_상품에_리뷰를_작성할_수_없다() {
        Client seller = client(1L, "seller@example.com", "판매자", "김판매");
        Item item = item(10L, seller, TradeType.AUCTION, TradeStatus.SOLD_OUT, false);

        given(itemService.getItem(10L)).willReturn(item);
        given(clientService.getClient(1L)).willReturn(seller);

        assertThatThrownBy(() -> reviewFacade.createReview(10L, 1L, new ReviewCreateRequest(5, "좋아요")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.SELF_REVIEW_NOT_ALLOWED));

        verify(reviewService, never()).createReview(any(), any(), any());
    }

    @DisplayName("낙찰자가 아니면 리뷰를 작성할 수 없다")
    @Test
    void 낙찰자가_아니면_리뷰를_작성할_수_없다() {
        Client seller = client(1L, "seller@example.com", "판매자", "김판매");
        Client buyer = client(2L, "buyer@example.com", "구매자", "박구매");
        Client other = client(3L, "other@example.com", "타인", "최타인");
        Item item = item(10L, seller, TradeType.AUCTION, TradeStatus.SOLD_OUT, false);

        given(itemService.getItem(10L)).willReturn(item);
        given(clientService.getClient(3L)).willReturn(other);
        given(auctionStatusService.getCurrentBidder(10L)).willReturn(buyer);

        assertThatThrownBy(() -> reviewFacade.createReview(10L, 3L, new ReviewCreateRequest(5, "좋아요")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_NOT_ALLOWED));

        verify(reviewService, never()).createReview(any(), any(), any());
    }

    private Item item(Long itemId, Client seller, TradeType tradeType, TradeStatus tradeStatus, boolean isDraft) {
        Item item = Item.builder()
                .category(Category.builder()
                        .name("디지털/가전")
                        .sortOrder(1)
                        .isActive(true)
                        .build())
                .seller(seller)
                .tradeType(tradeType)
                .title("중고 노트북")
                .description("상태 좋은 노트북입니다.")
                .initialPrice(100_000L)
                .conditionType(ConditionType.USED)
                .tradeStatus(tradeStatus)
                .isDraft(isDraft)
                .build();
        ReflectionTestUtils.setField(item, "id", itemId);
        return item;
    }

    private Client client(Long clientId, String email, String nickname, String name) {
        Client client = Client.create(
                email,
                "encodedPassword",
                nickname,
                name,
                "010-1234-5678");
        ReflectionTestUtils.setField(client, "id", clientId);
        return client;
    }
}

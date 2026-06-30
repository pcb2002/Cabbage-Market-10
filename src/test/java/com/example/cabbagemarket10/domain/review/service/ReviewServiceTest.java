package com.example.cabbagemarket10.domain.review.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.example.cabbagemarket10.domain.category.entity.Category;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.item.entity.Item;
import com.example.cabbagemarket10.domain.item.enums.ConditionType;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.enums.TradeType;
import com.example.cabbagemarket10.domain.review.dto.request.ReviewCreateRequest;
import com.example.cabbagemarket10.domain.review.dto.response.ReviewCreateResponse;
import com.example.cabbagemarket10.domain.review.entity.Review;
import com.example.cabbagemarket10.domain.review.repository.ReviewRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ReviewService reviewService;

    @DisplayName("리뷰를 저장하고 응답을 반환한다")
    @Test
    void 리뷰를_저장하고_응답을_반환한다() {
        Client seller = client(1L, "seller@example.com", "판매자", "김판매");
        Client buyer = client(2L, "buyer@example.com", "구매자", "박구매");
        Item item = item(10L, seller, TradeType.AUCTION, TradeStatus.SOLD_OUT, false);
        ReviewCreateRequest request = new ReviewCreateRequest(5, "좋은 거래였습니다.");
        LocalDateTime createdAt = LocalDateTime.of(2026, 6, 30, 12, 0);

        given(reviewRepository.existsByItem_IdAndReviewer_Id(10L, 2L)).willReturn(false);
        given(reviewRepository.saveAndFlush(any(Review.class))).willAnswer(invocation -> {
            Review review = invocation.getArgument(0);
            ReflectionTestUtils.setField(review, "id", 100L);
            ReflectionTestUtils.setField(review, "createdAt", createdAt);
            return review;
        });

        ReviewCreateResponse response = reviewService.createReview(item, buyer, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewRepository).saveAndFlush(captor.capture());
        Review savedReview = captor.getValue();
        assertThat(savedReview.getItem()).isSameAs(item);
        assertThat(savedReview.getReviewer()).isSameAs(buyer);
        assertThat(savedReview.getReviewee()).isSameAs(seller);
        assertThat(savedReview.getRating()).isEqualTo(5);
        assertThat(savedReview.getContent()).isEqualTo("좋은 거래였습니다.");

        assertThat(response.reviewId()).isEqualTo(100L);
        assertThat(response.itemId()).isEqualTo(10L);
        assertThat(response.reviewerId()).isEqualTo(2L);
        assertThat(response.revieweeId()).isEqualTo(1L);
    }

    @DisplayName("이미 리뷰를 작성한 상품에는 중복 리뷰를 작성할 수 없다")
    @Test
    void 이미_리뷰를_작성한_상품에는_중복_리뷰를_작성할_수_없다() {
        Client seller = client(1L, "seller@example.com", "판매자", "김판매");
        Client buyer = client(2L, "buyer@example.com", "구매자", "박구매");
        Item item = item(10L, seller, TradeType.AUCTION, TradeStatus.SOLD_OUT, false);

        given(reviewRepository.existsByItem_IdAndReviewer_Id(10L, 2L)).willReturn(true);

        assertThatThrownBy(() -> reviewService.createReview(item, buyer, new ReviewCreateRequest(5, "좋아요")))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.REVIEW_ALREADY_EXISTS));

        verify(reviewRepository, never()).saveAndFlush(any());
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

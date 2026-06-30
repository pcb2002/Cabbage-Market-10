package com.example.cabbagemarket10.domain.client.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

import com.example.cabbagemarket10.domain.client.dto.request.ClientMyInfoUpdateRequest;
import com.example.cabbagemarket10.domain.client.dto.response.ClientMyInfoResponse;
import com.example.cabbagemarket10.domain.client.dto.response.ClientProfileResponse;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.domain.follow.repository.FollowRepository;
import com.example.cabbagemarket10.domain.item.enums.TradeStatus;
import com.example.cabbagemarket10.domain.item.repository.ItemRepository;
import com.example.cabbagemarket10.domain.review.repository.ReviewRepository;
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
class ClientServiceTest {

    @Mock
    private ClientRepository clientRepository;

    @Mock
    private ItemRepository itemRepository;

    @Mock
    private FollowRepository followRepository;

    @Mock
    private ReviewRepository reviewRepository;

    @InjectMocks
    private ClientService clientService;

    @DisplayName("회원이 존재하면 공개 프로필과 상단 요약 정보를 DTO로 반환한다")
    @Test
    void 회원이_존재하면_공개_프로필과_상단_요약_정보를_DTO로_반환한다() {
        Client client = createClient(1L, "profile@example.com", "배추프로필", "홍길동", "010-1234-5678");

        given(clientRepository.findById(1L)).willReturn(Optional.of(client));
        given(reviewRepository.findAverageRatingByRevieweeId(1L)).willReturn(4.5);
        given(reviewRepository.countByReviewee_Id(1L)).willReturn(12L);
        given(followRepository.countByFollowing_Id(1L)).willReturn(20L);
        given(followRepository.existsByFollower_IdAndFollowing_Id(2L, 1L)).willReturn(true);
        given(itemRepository.countBySellerIdAndTradeStatusAndIsDraftFalse(1L, TradeStatus.ON_SALE)).willReturn(3L);
        given(itemRepository.countBySellerIdAndTradeStatusAndIsDraftFalse(1L, TradeStatus.SOLD_OUT)).willReturn(5L);

        ClientProfileResponse response = clientService.getClientProfile(1L, 2L);

        assertThat(response.clientId()).isEqualTo(1L);
        assertThat(response.nickname()).isEqualTo("배추프로필");
        assertThat(response.profileImageUrl()).isEqualTo(Client.defaultProfileImageUrl());
        assertThat(response.averageRating()).isEqualTo(4.5);
        assertThat(response.reviewCount()).isEqualTo(12L);
        assertThat(response.followerCount()).isEqualTo(20L);
        assertThat(response.isFollowing()).isTrue();
        assertThat(response.sellingItemCount()).isEqualTo(3L);
        assertThat(response.soldItemCount()).isEqualTo(5L);
    }

    @DisplayName("비회원 공개 프로필 조회 시 팔로우 여부는 false다")
    @Test
    void 비회원_공개_프로필_조회_시_팔로우_여부는_false다() {
        Client client = createClient(1L, "profile@example.com", "배추프로필", "홍길동", "010-1234-5678");

        given(clientRepository.findById(1L)).willReturn(Optional.of(client));
        given(reviewRepository.findAverageRatingByRevieweeId(1L)).willReturn(0.0);
        given(reviewRepository.countByReviewee_Id(1L)).willReturn(0L);
        given(followRepository.countByFollowing_Id(1L)).willReturn(0L);
        given(itemRepository.countBySellerIdAndTradeStatusAndIsDraftFalse(1L, TradeStatus.ON_SALE)).willReturn(0L);
        given(itemRepository.countBySellerIdAndTradeStatusAndIsDraftFalse(1L, TradeStatus.SOLD_OUT)).willReturn(0L);

        ClientProfileResponse response = clientService.getClientProfile(1L, null);

        assertThat(response.isFollowing()).isFalse();
    }

    @DisplayName("회원이 없으면 공개 프로필 조회 시 CLIENT_NOT_FOUND 예외가 발생한다")
    @Test
    void 회원이_없으면_공개_프로필_조회_시_CLIENT_NOT_FOUND_예외가_발생한다() {
        given(clientRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.getClientProfile(1L, null))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    @DisplayName("내 정보 수정 시 전달된 필드만 변경한다")
    @Test
    void 내_정보_수정_시_전달된_필드만_변경한다() {
        Client client = createClient(1L, "client@example.com", "beforeNickname", "기존이름", "010-1234-5678");
        ClientMyInfoUpdateRequest request = new ClientMyInfoUpdateRequest(
                "afterNickname",
                null,
                null,
                "http://localhost:9000/me.png"
        );

        given(clientRepository.findById(1L)).willReturn(Optional.of(client));

        ClientMyInfoResponse response = clientService.updateMyInfo(1L, request);

        assertThat(response.nickname()).isEqualTo("afterNickname");
        assertThat(response.name()).isEqualTo("기존이름");
        assertThat(response.phone()).isEqualTo("010-1234-5678");
        assertThat(response.profileImageUrl()).isEqualTo("http://localhost:9000/me.png");
        assertThat(client.getNickname()).isEqualTo("afterNickname");
        assertThat(client.getName()).isEqualTo("기존이름");
    }

    @DisplayName("내 정보 수정 시 회원이 없으면 CLIENT_NOT_FOUND 예외가 발생한다")
    @Test
    void 내_정보_수정_시_회원이_없으면_CLIENT_NOT_FOUND_예외가_발생한다() {
        ClientMyInfoUpdateRequest request = new ClientMyInfoUpdateRequest(
                "afterNickname",
                null,
                null,
                null
        );

        given(clientRepository.findById(1L)).willReturn(Optional.empty());

        assertThatThrownBy(() -> clientService.updateMyInfo(1L, request))
                .isInstanceOfSatisfying(BusinessException.class, exception ->
                        assertThat(exception.getErrorCode()).isEqualTo(ErrorCode.CLIENT_NOT_FOUND));
    }

    private Client createClient(Long id, String email, String nickname, String name, String phone) {
        Client client = Client.create(
                email,
                "encodedPassword",
                nickname,
                name,
                phone);
        ReflectionTestUtils.setField(client, "id", id);
        return client;
    }
}

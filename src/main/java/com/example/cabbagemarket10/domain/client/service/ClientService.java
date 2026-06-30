package com.example.cabbagemarket10.domain.client.service;

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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;
    private final ItemRepository itemRepository;
    private final FollowRepository followRepository;
    private final ReviewRepository reviewRepository;

    @Transactional(readOnly = true)
    public ClientMyInfoResponse getMyInfo(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        return ClientMyInfoResponse.from(client);
    }

    @Transactional(readOnly = true)
    public ClientProfileResponse getClientProfile(Long clientId) {
        return getClientProfile(clientId, null);
    }

    @Transactional
    public ClientMyInfoResponse updateMyInfo(Long clientId, ClientMyInfoUpdateRequest request) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));

        client.updateProfile(
                request.nickname(),
                request.name(),
                request.phone(),
                request.profileImageUrl()
        );

        return ClientMyInfoResponse.from(client);

    }

    @Transactional(readOnly = true)
    public ClientProfileResponse getClientProfile(Long clientId, Long viewerClientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));
        double averageRating = reviewRepository.findAverageRatingByRevieweeId(clientId);
        long reviewCount = reviewRepository.countByReviewee_Id(clientId);
        long followerCount = followRepository.countByFollowing_Id(clientId);
        boolean isFollowing = viewerClientId != null
                && !viewerClientId.equals(clientId)
                && followRepository.existsByFollower_IdAndFollowing_Id(viewerClientId, clientId);
        long sellingItemCount = itemRepository.countBySellerIdAndTradeStatusAndIsDraftFalse(
                clientId,
                TradeStatus.ON_SALE);
        long soldItemCount = itemRepository.countBySellerIdAndTradeStatusAndIsDraftFalse(
                clientId,
                TradeStatus.SOLD_OUT);

        return ClientProfileResponse.from(
                client,
                averageRating,
                reviewCount,
                followerCount,
                isFollowing,
                sellingItemCount,
                soldItemCount
        );
    }

    public Client getClient(Long clientId) {
        return clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CLIENT_NOT_FOUND));
    }
}

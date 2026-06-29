package com.example.cabbagemarket10.domain.client.dto.response;

import com.example.cabbagemarket10.domain.client.entity.Client;

public record ClientProfileResponse(
        Long clientId,
        String nickname,
        String profileImageUrl,
        double averageRating,
        long reviewCount,
        long followerCount,
        boolean isFollowing,
        long sellingItemCount,
        long soldItemCount
) {

    public static ClientProfileResponse from(
            Client client,
            double averageRating,
            long reviewCount,
            long followerCount,
            boolean isFollowing,
            long sellingItemCount,
            long soldItemCount
    ) {
        return new ClientProfileResponse(
                client.getId(),
                client.getNickname(),
                client.getProfileImageUrl(),
                averageRating,
                reviewCount,
                followerCount,
                isFollowing,
                sellingItemCount,
                soldItemCount
        );
    }
}

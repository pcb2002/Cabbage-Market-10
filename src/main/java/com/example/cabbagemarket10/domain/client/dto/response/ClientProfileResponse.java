package com.example.cabbagemarket10.domain.client.dto.response;

import com.example.cabbagemarket10.domain.client.entity.Client;

public record ClientProfileResponse(
        Long clientId,
        String nickname,
        String profileImageUrl
) {

    public static ClientProfileResponse from(Client client) {
        return new ClientProfileResponse(
                client.getId(),
                client.getNickname(),
                client.getProfileImageUrl()
        );
    }
}

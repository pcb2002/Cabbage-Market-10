package com.example.cabbagemarket10.domain.client.dto.response;

import com.example.cabbagemarket10.domain.client.entity.AccountStatus;
import com.example.cabbagemarket10.domain.client.entity.Client;
import java.time.LocalDateTime;

public record ClientMyInfoResponse(
        Long clientId,
        String email,
        String nickname,
        String name,
        String phone,
        String profileImageUrl,
        AccountStatus status,
        boolean verified,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {

    public static ClientMyInfoResponse from(Client client) {
        return new ClientMyInfoResponse(
                client.getId(),
                client.getEmail(),
                client.getNickname(),
                client.getName(),
                client.getPhone(),
                client.getProfileImageUrl(),
                client.getStatus(),
                client.isVerified(),
                client.getCreatedAt(),
                client.getUpdatedAt()
        );
    }
}

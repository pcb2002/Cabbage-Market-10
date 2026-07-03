package com.example.cabbagemarket10.domain.auth.dto.response;

import com.example.cabbagemarket10.domain.client.entity.Client;

import java.time.LocalDateTime;

public record SignupResponse(
        Long clientId,
        String email,
        String nickname,
        LocalDateTime createdAt) {

    public static SignupResponse from(Client client) {
        return new SignupResponse(
                client.getId(),
                client.getEmail(),
                client.getNickname(),
                client.getCreatedAt());
    }
}

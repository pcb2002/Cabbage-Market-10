package com.example.cabbagemarket10.domain.auth.dto.response;

public record LoginResponse(
        String accessToken,
        String refreshToken) {

    public static LoginResponse of(String accessToken, String refreshToken) {
        return new LoginResponse(accessToken, refreshToken);
    }
}

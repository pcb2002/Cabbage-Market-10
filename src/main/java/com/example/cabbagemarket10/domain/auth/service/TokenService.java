package com.example.cabbagemarket10.domain.auth.service;

import com.example.cabbagemarket10.domain.auth.dto.response.LoginResponse;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.global.security.jwt.JwtClaims;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TokenService {

    private final JwtTokenProvider jwtTokenProvider;

    public LoginResponse issueTokens(Client client) {
        return LoginResponse.of(
                jwtTokenProvider.createAccessToken(client),
                jwtTokenProvider.createRefreshToken(client));
    }

    public JwtClaims validateRefreshToken(String refreshToken) {
        return jwtTokenProvider.validateRefreshToken(refreshToken);
    }
}

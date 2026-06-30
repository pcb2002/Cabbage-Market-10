package com.example.cabbagemarket10.application.facade;

import com.example.cabbagemarket10.domain.auth.dto.request.LoginRequest;
import com.example.cabbagemarket10.domain.auth.dto.request.SignupRequest;
import com.example.cabbagemarket10.domain.auth.dto.response.LoginResponse;
import com.example.cabbagemarket10.domain.auth.dto.response.SignupResponse;
import com.example.cabbagemarket10.domain.auth.service.AuthService;
import com.example.cabbagemarket10.domain.auth.service.BlackListService;
import com.example.cabbagemarket10.domain.auth.service.TokenService;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.global.security.jwt.JwtClaims;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthFacade {

    private final AuthService authService;
    private final TokenService tokenService;
    private final BlackListService blackListService;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        return authService.signup(request);
    }

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Client client = authService.authenticate(request);
        return tokenService.issueTokens(client);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        JwtClaims claims = tokenService.validateRefreshToken(refreshToken);
        Client client = authService.getActiveClient(claims.clientId());
        return tokenService.issueTokens(client);
    }

    @Transactional
    public void logout(String accessToken) {
        blackListService.blacklistAccessToken(accessToken);
    }
}

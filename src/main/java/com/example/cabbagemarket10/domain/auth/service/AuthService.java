package com.example.cabbagemarket10.domain.auth.service;

import com.example.cabbagemarket10.domain.auth.dto.request.LoginRequest;
import com.example.cabbagemarket10.domain.auth.dto.request.SignupRequest;
import com.example.cabbagemarket10.domain.auth.dto.response.LoginResponse;
import com.example.cabbagemarket10.domain.auth.dto.response.SignupResponse;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.jwt.JwtClaims;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final ClientRepository clientRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistStore tokenBlacklistStore;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (clientRepository.countByEmailIncludingDeleted(request.email()) > 0) {
            throw new BusinessException(ErrorCode.DUPLICATED_EMAIL);
        }

        Client client = Client.create(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.nickname(),
                request.name(),
                request.phone());

        return SignupResponse.from(clientRepository.save(client));
    }


    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Client client = getActiveClientByEmail(request.email());
        if (!passwordEncoder.matches(request.password(), client.getPassword())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }
        return issueTokens(client);
    }

    @Transactional
    public LoginResponse refresh(String refreshToken) {
        JwtClaims refreshClaims = jwtTokenProvider.validateRefreshToken(refreshToken);
        validateNotBlacklisted(refreshClaims.jti());

        Client client = getActiveClientById(refreshClaims.clientId());
        tokenBlacklistStore.blacklist(refreshClaims.jti(), refreshClaims.expiresAt());

        return issueTokens(client);
    }

    public void logout(String accessToken, String refreshToken) {
        JwtClaims accessClaims = jwtTokenProvider.validateAccessToken(accessToken);
        validateNotBlacklisted(accessClaims.jti());
        tokenBlacklistStore.blacklist(accessClaims.jti(), accessClaims.expiresAt());

        Optional.ofNullable(refreshToken)
                .filter(token -> !token.isBlank())
                .ifPresent(this::tryBlacklistRefreshToken);
    }

    // RefreshToken이 이미 만료됐거나 유효하지 않으면 무시한다.
    // AccessToken이 이미 블랙리스트에 등록된 시점에서 RefreshToken 처리 실패는 로그아웃을 실패로 만들 이유가 없다.
    private void tryBlacklistRefreshToken(String refreshToken) {
        try {
            JwtClaims refreshClaims = jwtTokenProvider.validateRefreshToken(refreshToken);
            validateNotBlacklisted(refreshClaims.jti());
            tokenBlacklistStore.blacklist(refreshClaims.jti(), refreshClaims.expiresAt());
        } catch (BusinessException ignored) {
        }
    }

    private void validateNotBlacklisted(String jti) {
        if (tokenBlacklistStore.isBlacklisted(jti)) {
            throw new BusinessException(ErrorCode.BLACKLISTED_TOKEN);
        }
    }

    private LoginResponse issueTokens(Client client) {
        return LoginResponse.of(
                jwtTokenProvider.createAccessToken(client),
                jwtTokenProvider.createRefreshToken(client));
    }

    private Client getActiveClientByEmail(String email) {
        Client client = clientRepository.findByEmail(email)
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));
        validateActiveClient(client);
        return client;
    }

    private Client getActiveClientById(Long clientId) {
        Client client = clientRepository.findById(clientId)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN));
        validateActiveClient(client);
        return client;
    }

    private void validateActiveClient(Client client) {
        if (!client.isActive()) {
            throw new BusinessException(ErrorCode.SUSPENDED_ACCOUNT);
        }
    }
}

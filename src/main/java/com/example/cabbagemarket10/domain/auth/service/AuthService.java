package com.example.cabbagemarket10.domain.auth.service;

import com.example.cabbagemarket10.domain.auth.dto.request.LoginRequest;
import com.example.cabbagemarket10.domain.auth.dto.request.SignupRequest;
import com.example.cabbagemarket10.domain.auth.dto.response.SignupResponse;
import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.domain.client.repository.ClientRepository;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
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
    public String login(LoginRequest request) {
        Client client = clientRepository.findByEmail(request.email())
                .orElseThrow(() -> new BusinessException(ErrorCode.LOGIN_FAILED));

        if (!client.isCorrectPassword(passwordEncoder, request.password())) {
            throw new BusinessException(ErrorCode.LOGIN_FAILED);
        }

        if (!client.isActive()) {
            throw new BusinessException(ErrorCode.SUSPENDED_ACCOUNT);
        }

        return jwtTokenProvider.createAccessToken(client);
    }
}

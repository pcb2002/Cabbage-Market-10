package com.example.cabbagemarket10.domain.chat.util;

import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class JwtUtil {

    public static final String BEARER_PREFIX = "Bearer ";
    private final JwtTokenProvider jwtTokenProvider;

    public Long getUserId(String token) {
        return jwtTokenProvider.validateAccessToken(token).clientId();
    }
}

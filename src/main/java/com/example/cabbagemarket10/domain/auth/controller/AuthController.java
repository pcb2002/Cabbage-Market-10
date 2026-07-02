package com.example.cabbagemarket10.domain.auth.controller;

import com.example.cabbagemarket10.application.facade.AuthFacade;
import com.example.cabbagemarket10.domain.auth.dto.request.LoginRequest;
import com.example.cabbagemarket10.domain.auth.dto.request.SignupRequest;
import com.example.cabbagemarket10.domain.auth.dto.response.LoginResponse;
import com.example.cabbagemarket10.domain.auth.dto.response.SignupResponse;
import com.example.cabbagemarket10.domain.auth.service.AuthCookieManager;
import com.example.cabbagemarket10.global.common.response.CommonResponse;
import com.example.cabbagemarket10.global.security.jwt.JwtTokenProvider;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/auth")
public class AuthController {

    private static final String BEARER_PREFIX = "Bearer ";

    private final AuthFacade authFacade;
    private final AuthCookieManager authCookieManager;
    private final JwtTokenProvider jwtTokenProvider;

    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        return CommonResponse.success(HttpStatus.CREATED, authFacade.signup(request))
                .toResponseEntity();
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<Void>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse tokens = authFacade.login(request);
        HttpHeaders headers = authCookieManager.createRefreshTokenHeaders(tokens.refreshToken());
        headers.add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + tokens.accessToken());
        return CommonResponse.success(HttpStatus.OK)
                .toResponseEntity(headers);
    }

    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<Void>> refresh(
            @CookieValue(name = AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        LoginResponse tokens = authFacade.refresh(authCookieManager.requireRefreshToken(refreshToken));
        HttpHeaders headers = authCookieManager.createRefreshTokenHeaders(tokens.refreshToken());
        headers.add(HttpHeaders.AUTHORIZATION, BEARER_PREFIX + tokens.accessToken());
        return CommonResponse.success(HttpStatus.OK)
                .toResponseEntity(headers);
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader) {
        authFacade.logout(jwtTokenProvider.extractAccessTokenFromHeader(authorizationHeader));
        return CommonResponse.success(HttpStatus.OK)
                .toResponseEntity(authCookieManager.createExpiredRefreshTokenHeaders());
    }

}

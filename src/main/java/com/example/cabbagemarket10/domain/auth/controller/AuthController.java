package com.example.cabbagemarket10.domain.auth.controller;

import com.example.cabbagemarket10.domain.auth.dto.request.LoginRequest;
import com.example.cabbagemarket10.domain.auth.dto.request.SignupRequest;
import com.example.cabbagemarket10.domain.auth.dto.response.LoginResponse;
import com.example.cabbagemarket10.domain.auth.dto.response.SignupResponse;
import com.example.cabbagemarket10.domain.auth.service.AuthCookieManager;
import com.example.cabbagemarket10.domain.auth.service.AuthService;
import com.example.cabbagemarket10.global.common.CommonResponse;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
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

    private final AuthService authService;
    private final AuthCookieManager authCookieManager;

    @PostMapping("/signup")
    public ResponseEntity<CommonResponse<SignupResponse>> signup(
            @Valid @RequestBody SignupRequest request) {
        return CommonResponse.success(HttpStatus.CREATED, authService.signup(request))
                .toResponseEntity();
    }

    @PostMapping("/login")
    public ResponseEntity<CommonResponse<Void>> login(
            @Valid @RequestBody LoginRequest request) {
        LoginResponse tokens = authService.login(request);
        return CommonResponse.success(HttpStatus.OK).toResponseEntity(createAuthHeaders(tokens));
    }

    @PostMapping("/refresh")
    public ResponseEntity<CommonResponse<Void>> refresh(
            @CookieValue(name = AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        LoginResponse tokens = authService.refresh(requireRefreshToken(refreshToken));
        return CommonResponse.success(HttpStatus.OK).toResponseEntity(createAuthHeaders(tokens));
    }

    @PostMapping("/logout")
    public ResponseEntity<CommonResponse<Void>> logout(
            @RequestHeader(name = HttpHeaders.AUTHORIZATION, required = false) String authorizationHeader,
            @CookieValue(name = AuthCookieManager.REFRESH_TOKEN_COOKIE_NAME, required = false) String refreshToken) {
        authService.logout(extractAccessToken(authorizationHeader), refreshToken);

        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.SET_COOKIE, authCookieManager.expireRefreshTokenCookie().toString());
        return CommonResponse.success(HttpStatus.OK).toResponseEntity(headers);
    }

    private HttpHeaders createAuthHeaders(LoginResponse tokens) {
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.AUTHORIZATION, "Bearer " + tokens.accessToken());
        headers.add(HttpHeaders.SET_COOKIE, authCookieManager.createRefreshTokenCookie(tokens.refreshToken()).toString());
        return headers;
    }

    private String requireRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            throw new BusinessException(ErrorCode.INVALID_REFRESH_TOKEN);
        }
        return refreshToken;
    }

    private String extractAccessToken(String authorizationHeader) {
        if (authorizationHeader == null || authorizationHeader.isBlank()) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_MISSING);
        }
        if (!authorizationHeader.startsWith("Bearer ")) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
        return authorizationHeader.substring("Bearer ".length());
    }
}

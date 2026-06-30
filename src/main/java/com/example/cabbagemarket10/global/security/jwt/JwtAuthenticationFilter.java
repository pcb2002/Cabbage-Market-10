package com.example.cabbagemarket10.global.security.jwt;

import com.example.cabbagemarket10.domain.auth.service.TokenBlacklistStore;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.example.cabbagemarket10.global.security.SecurityErrorResponseWriter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    private final JwtTokenProvider jwtTokenProvider;
    private final TokenBlacklistStore tokenBlacklistStore;
    private final SecurityErrorResponseWriter securityErrorResponseWriter;

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        String authorizationHeader = request.getHeader(AUTHORIZATION_HEADER);

        if (!hasBearerToken(authorizationHeader)) {
            filterChain.doFilter(request, response);
            return;
        }

        try {
            JwtClaims claims = jwtTokenProvider.validateAccessToken(extractAccessToken(authorizationHeader));
            validateNotBlacklisted(claims.jti());
            validateNotSuspendedClient(claims);
            AuthenticatedClient principal = new AuthenticatedClient(claims.clientId(), claims.email());
            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            List.of(new SimpleGrantedAuthority(claims.role())));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            filterChain.doFilter(request, response);
        } catch (BusinessException exception) {
            SecurityContextHolder.clearContext();
            securityErrorResponseWriter.write(response, exception.getErrorCode());
        }
    }

    private boolean hasBearerToken(String authorizationHeader) {
        return StringUtils.hasText(authorizationHeader)
                && authorizationHeader.startsWith(BEARER_PREFIX);
    }

    private String extractAccessToken(String authorizationHeader) {
        return authorizationHeader.substring(BEARER_PREFIX.length());
    }

    private void validateNotBlacklisted(String jti) {
        if (tokenBlacklistStore.isBlacklisted(jti)) {
            throw new BusinessException(ErrorCode.BLACKLISTED_TOKEN);
        }
    }

    private void validateNotSuspendedClient(JwtClaims claims) {
        if (!tokenBlacklistStore.isSuspendedClientMarked(claims.clientId())) {
            return;
        }
        tokenBlacklistStore.blacklist(claims.jti(), claims.expiresAt());
        throw new BusinessException(ErrorCode.SUSPENDED_ACCOUNT);
    }
}

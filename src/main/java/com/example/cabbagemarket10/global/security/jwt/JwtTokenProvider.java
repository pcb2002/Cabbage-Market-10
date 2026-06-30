package com.example.cabbagemarket10.global.security.jwt;

import com.example.cabbagemarket10.domain.client.entity.Client;
import com.example.cabbagemarket10.global.exception.BusinessException;
import com.example.cabbagemarket10.global.exception.ErrorCode;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String ROLE_CLIENT = "ROLE_CLIENT";
    private static final String EMAIL_CLAIM = "email";
    private static final String ROLE_CLAIM = "role";
    private static final String TOKEN_TYPE_CLAIM = "tokenType";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Client client) {
        return createToken(client, ACCESS_TOKEN_TYPE, jwtProperties.accessTokenExpireSeconds());
    }

    public String createRefreshToken(Client client) {
        return createToken(client, REFRESH_TOKEN_TYPE, jwtProperties.refreshTokenExpireSeconds());
    }

    public JwtClaims validateAccessToken(String token) {
        return validateToken(token, ACCESS_TOKEN_TYPE, ErrorCode.ACCESS_TOKEN_EXPIRED, ErrorCode.ACCESS_TOKEN_INVALID);
    }

    public JwtClaims validateRefreshToken(String token) {
        return validateToken(token, REFRESH_TOKEN_TYPE, ErrorCode.REFRESH_TOKEN_EXPIRED, ErrorCode.INVALID_REFRESH_TOKEN);
    }

    private String createToken(Client client, String tokenType, long expireSeconds) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(expireSeconds);

        return Jwts.builder()
                .subject(String.valueOf(client.getId()))
                .claim(EMAIL_CLAIM, client.getEmail())
                .claim(ROLE_CLAIM, ROLE_CLIENT)
                .claim(TOKEN_TYPE_CLAIM, tokenType)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    private JwtClaims validateToken(
            String token,
            String expectedTokenType,
            ErrorCode expiredErrorCode,
            ErrorCode invalidErrorCode) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            validateTokenType(claims, expectedTokenType, invalidErrorCode);

            return new JwtClaims(
                    readClientId(claims, invalidErrorCode),
                    readString(claims, EMAIL_CLAIM, invalidErrorCode),
                    readString(claims, ROLE_CLAIM, invalidErrorCode),
                    readJti(claims, invalidErrorCode),
                    claims.getIssuedAt().toInstant(),
                    claims.getExpiration().toInstant());
        } catch (ExpiredJwtException exception) {
            throw new BusinessException(expiredErrorCode);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(invalidErrorCode);
        }
    }

    private void validateTokenType(Claims claims, String expectedTokenType, ErrorCode invalidErrorCode) {
        String tokenType = readString(claims, TOKEN_TYPE_CLAIM, invalidErrorCode);
        if (!expectedTokenType.equals(tokenType)) {
            throw new BusinessException(invalidErrorCode);
        }
    }

    private Long readClientId(Claims claims, ErrorCode invalidErrorCode) {
        try {
            String subject = claims.getSubject();
            if (subject == null) {
                throw new BusinessException(invalidErrorCode);
            }
            return Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new BusinessException(invalidErrorCode);
        }
    }

    private String readJti(Claims claims, ErrorCode invalidErrorCode) {
        String jti = claims.getId();
        if (jti == null) {
            throw new BusinessException(invalidErrorCode);
        }
        return jti;
    }

    private String readString(Claims claims, String key, ErrorCode invalidErrorCode) {
        Object value = claims.get(key);
        if (value == null) {
            throw new BusinessException(invalidErrorCode);
        }
        return String.valueOf(value);
    }
}

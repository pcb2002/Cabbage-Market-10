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

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(Client client) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(jwtProperties.accessTokenExpireSeconds());

        return Jwts.builder()
                .subject(String.valueOf(client.getId()))
                .claim(EMAIL_CLAIM, client.getEmail())
                .claim(ROLE_CLAIM, ROLE_CLIENT)
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiresAt))
                .signWith(secretKey)
                .compact();
    }

    public JwtClaims validateAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .clock(() -> Date.from(Instant.now(clock)))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return new JwtClaims(
                    readClientId(claims),
                    readString(claims, EMAIL_CLAIM),
                    readString(claims, ROLE_CLAIM),
                    readJti(claims),
                    claims.getIssuedAt().toInstant(),
                    claims.getExpiration().toInstant());
        } catch (ExpiredJwtException exception) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_EXPIRED);
        } catch (JwtException | IllegalArgumentException exception) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }

    private Long readClientId(Claims claims) {
        try {
            String subject = claims.getSubject();
            if (subject == null) {
                throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
            }
            return Long.parseLong(subject);
        } catch (NumberFormatException exception) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
    }

    private String readJti(Claims claims) {
        String jti = claims.getId();
        if (jti == null) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
        return jti;
    }

    private String readString(Claims claims, String key) {
        Object value = claims.get(key);
        if (value == null) {
            throw new BusinessException(ErrorCode.ACCESS_TOKEN_INVALID);
        }
        return String.valueOf(value);
    }
}

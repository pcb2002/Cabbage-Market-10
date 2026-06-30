package com.example.cabbagemarket10.domain.chat.util;

import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;
    public static final String BEARER_PREFIX = "Bearer ";
    private SecretKey key;
    private JwtParser parser;

    @PostConstruct
    public void init() {
        byte[] bytes = Decoders.BASE64.decode(secretKey);
        this.key = Keys.hmacShaKeyFor(bytes);
        this.parser = Jwts.parser().verifyWith(this.key).build();
    }

    public Long getUserId(String token) {
        return parser.parseSignedClaims(token).getPayload
                ().get("userId", Long.class);
    }


}

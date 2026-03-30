package com.smartstock.util;

import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import com.smartstock.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;

@Component
public class JwtUtil {

    private static final String BLACKLIST_PREFIX = "auth:token:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtProperties jwtProperties;
    private SecretKey secretKey;

    public JwtUtil(StringRedisTemplate stringRedisTemplate, JwtProperties jwtProperties) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.jwtProperties = jwtProperties;
    }

    @PostConstruct
    public void init() {
        String secret = jwtProperties.getSecret();
        if (secret == null || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("jwt.secret 至少需要 32 个字节");
        }
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    public String generateToken(Long userId, String email) {
        Instant now = Instant.now();
        Instant expiration = now.plusSeconds(jwtProperties.getExpirationSeconds());
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("userId", userId)
                .claim("email", email)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiration))
                .signWith(secretKey)
                .compact();
    }

    public Claims parseToken(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (JwtException | IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效或已过期的访问令牌");
        }
    }

    public Long getUserId(Claims claims) {
        Object claimValue = claims.get("userId");
        if (claimValue instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(claims.getSubject());
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "令牌中的用户信息无效");
        }
    }

    public void invalidateToken(String token) {
        Claims claims = parseToken(token);
        long remainingSeconds = Duration.between(Instant.now(), claims.getExpiration().toInstant()).getSeconds();
        if (remainingSeconds <= 0) {
            return;
        }
        stringRedisTemplate.opsForValue().set(
                BLACKLIST_PREFIX + token,
                "1",
                Duration.ofSeconds(remainingSeconds)
        );
    }

    public boolean isTokenBlacklisted(String token) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + token));
    }

    public long getExpirationSeconds() {
        return jwtProperties.getExpirationSeconds();
    }
}

package com.smartstock.util;

import com.smartstock.config.JwtProperties;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.startsWith;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtUtilTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("smartstock-ai-test-secret-key-change-in-production-2026");
        jwtProperties.setExpirationSeconds(3600);

        jwtUtil = new JwtUtil(stringRedisTemplate, jwtProperties);
        jwtUtil.init();
    }

    @Test
    void shouldGenerateAndParseToken() {
        String token = jwtUtil.generateToken(12L, "user@example.com");

        Claims claims = jwtUtil.parseToken(token);

        assertEquals(12L, jwtUtil.getUserId(claims));
        assertEquals("user@example.com", claims.get("email", String.class));
    }

    @Test
    void shouldStoreBlacklistedTokenWithRemainingExpiration() {
        String token = jwtUtil.generateToken(18L, "logout@example.com");

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);

        jwtUtil.invalidateToken(token);

        verify(valueOperations).set(startsWith("auth:token:blacklist:"), eq("1"), any(Duration.class));
    }
}

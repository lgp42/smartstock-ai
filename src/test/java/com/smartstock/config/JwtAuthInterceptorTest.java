package com.smartstock.config;

import com.smartstock.common.BusinessException;
import com.smartstock.util.JwtUtil;
import com.smartstock.util.UserContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class JwtAuthInterceptorTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    private JwtUtil jwtUtil;
    private JwtAuthInterceptor jwtAuthInterceptor;

    @BeforeEach
    void setUp() {
        JwtProperties jwtProperties = new JwtProperties();
        jwtProperties.setSecret("smartstock-ai-test-secret-key-change-in-production-2026");
        jwtProperties.setExpirationSeconds(3600);

        when(stringRedisTemplate.hasKey(anyString())).thenReturn(false);

        jwtUtil = new JwtUtil(stringRedisTemplate, jwtProperties);
        jwtUtil.init();
        jwtAuthInterceptor = new JwtAuthInterceptor(jwtUtil);
    }

    @Test
    void shouldSetRequestAttributesWhenTokenIsValid() throws Exception {
        String token = jwtUtil.generateToken(7L, "valid@example.com");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + token);

        boolean handled = jwtAuthInterceptor.preHandle(request, new MockHttpServletResponse(), new Object());

        assertTrue(handled);
        assertEquals(7L, request.getAttribute(UserContext.REQUEST_ATTR_USER_ID));
        assertEquals(token, request.getAttribute(UserContext.REQUEST_ATTR_TOKEN));
    }

    @Test
    void shouldRejectInvalidToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token");

        assertThrows(BusinessException.class,
                () -> jwtAuthInterceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
    }
}

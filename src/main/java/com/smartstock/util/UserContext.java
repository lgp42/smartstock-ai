package com.smartstock.util;

import com.smartstock.common.BusinessException;
import com.smartstock.common.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.util.StringUtils;

public class UserContext {

    public static final String REQUEST_ATTR_USER_ID = "currentUserId";
    public static final String REQUEST_ATTR_TOKEN = "currentToken";

    private UserContext() {
        // Utility class
    }

    /**
     * 从 JWT 鉴权拦截器注入的请求属性中获取当前用户 ID
     */
    public static Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(REQUEST_ATTR_USER_ID);
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "请先登录");
        }
        if (userId instanceof Number number) {
            return number.longValue();
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的用户标识");
    }

    public static String getToken(HttpServletRequest request) {
        Object token = request.getAttribute(REQUEST_ATTR_TOKEN);
        if (token instanceof String value && StringUtils.hasText(value)) {
            return value;
        }
        throw new BusinessException(ErrorCode.UNAUTHORIZED, "无效的访问令牌");
    }
}

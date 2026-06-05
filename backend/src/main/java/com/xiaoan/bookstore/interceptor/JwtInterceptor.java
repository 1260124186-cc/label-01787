package com.xiaoan.bookstore.interceptor;

import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.util.JwtUtil;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null || !jwtUtil.isValid(token)) {
            throw new BusinessException(401, "未登录或Token已过期");
        }

        request.setAttribute(Constants.CONTEXT_USER_ID, jwtUtil.getUserId(token));
        request.setAttribute(Constants.CONTEXT_USER_TYPE, jwtUtil.getUserType(token));
        return true;
    }

    /**
     * 从 Header 或 URL 参数中提取 Token（兼容 image/文件类资源请求）
     */
    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.TOKEN_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(Constants.TOKEN_PREFIX)) {
            return header.substring(Constants.TOKEN_PREFIX.length());
        }
        // 兼容图片等资源请求通过 URL 参数传 token
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }
        return null;
    }
}

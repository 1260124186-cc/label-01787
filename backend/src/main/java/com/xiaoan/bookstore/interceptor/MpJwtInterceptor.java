package com.xiaoan.bookstore.interceptor;

import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.util.JwtUtil;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class MpJwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null || !jwtUtil.isMpTokenValid(token)) {
            throw new BusinessException(401, "未登录或Token已过期");
        }

        Claims claims = jwtUtil.parseMpToken(token);
        Long userId = jwtUtil.getUserId(claims);
        Integer userType = jwtUtil.getUserType(claims);

        if (userType != Constants.USER_TYPE_MP) {
            throw new BusinessException(403, "无权访问小程序接口");
        }

        request.setAttribute(Constants.CONTEXT_USER_ID, userId);
        request.setAttribute(Constants.CONTEXT_USER_TYPE, userType);

        TenantContext.set(userId, userType, null);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }

    private String resolveToken(HttpServletRequest request) {
        String header = request.getHeader(Constants.TOKEN_HEADER);
        if (StringUtils.hasText(header) && header.startsWith(Constants.TOKEN_PREFIX)) {
            return header.substring(Constants.TOKEN_PREFIX.length());
        }
        String paramToken = request.getParameter("token");
        if (StringUtils.hasText(paramToken)) {
            return paramToken;
        }
        return null;
    }
}

package com.xiaoan.bookstore.interceptor;

import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.entity.AdminUser;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.mapper.AdminUserMapper;
import com.xiaoan.bookstore.service.RbacService;
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
public class AdminJwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final RbacService rbacService;
    private final AdminUserMapper adminUserMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String token = resolveToken(request);
        if (token == null || !jwtUtil.isAdminTokenValid(token)) {
            throw new BusinessException(401, "未登录或Token已过期");
        }

        Claims claims = jwtUtil.parseAdminToken(token);
        Long userId = jwtUtil.getUserId(claims);
        Integer userType = jwtUtil.getUserType(claims);
        Long roleId = jwtUtil.getRoleId(claims);

        if (userType != Constants.USER_TYPE_ADMIN) {
            throw new BusinessException(403, "无权访问管理端接口");
        }

        AdminUser admin = adminUserMapper.selectById(userId);
        if (admin == null || admin.getStatus() != Constants.STATUS_ENABLED) {
            throw new BusinessException(401, "账号已被禁用或不存在");
        }

        request.setAttribute(Constants.CONTEXT_USER_ID, userId);
        request.setAttribute(Constants.CONTEXT_USER_TYPE, userType);
        request.setAttribute(Constants.CONTEXT_ROLE_ID, roleId);
        request.setAttribute(Constants.CONTEXT_ROLE_CODE, getRoleCode(roleId));

        TenantContext.set(userId, userType, roleId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        TenantContext.clear();
    }

    private String getRoleCode(Long roleId) {
        if (roleId == null) return null;
        var role = rbacService.getRoleWithPermissions(roleId);
        return role != null ? role.getCode() : null;
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

package com.xiaoan.bookstore.aspect;

import com.xiaoan.bookstore.annotation.RequirePermission;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.service.RbacService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class PermissionAspect {

    private final RbacService rbacService;

    @Before("@annotation(requirePermission)")
    public void checkPermission(JoinPoint joinPoint, RequirePermission requirePermission) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;

        HttpServletRequest request = attrs.getRequest();
        Long roleId = (Long) request.getAttribute(Constants.CONTEXT_ROLE_ID);
        String roleCode = (String) request.getAttribute(Constants.CONTEXT_ROLE_CODE);

        if (Constants.ROLE_SUPER_ADMIN.equals(roleCode)) {
            return;
        }

        String requiredPermission = requirePermission.value();
        if (!rbacService.hasPermission(roleId, requiredPermission)) {
            throw new BusinessException(403, "权限不足: " + requiredPermission);
        }
    }
}

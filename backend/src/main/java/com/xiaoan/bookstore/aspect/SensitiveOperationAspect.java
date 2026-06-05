package com.xiaoan.bookstore.aspect;

import com.xiaoan.bookstore.annotation.SensitiveOperation;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.exception.BusinessException;
import com.xiaoan.bookstore.service.SensitiveOperationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Aspect
@Component
@RequiredArgsConstructor
public class SensitiveOperationAspect {

    private final SensitiveOperationService sensitiveOperationService;

    @Before("@annotation(sensitiveOp)")
    public void verifyConfirmation(JoinPoint joinPoint, SensitiveOperation sensitiveOp) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;

        HttpServletRequest request = attrs.getRequest();
        Long userId = (Long) request.getAttribute(Constants.CONTEXT_USER_ID);
        String roleCode = (String) request.getAttribute(Constants.CONTEXT_ROLE_CODE);

        if (Constants.ROLE_SUPER_ADMIN.equals(roleCode)) {
            return;
        }

        String confirmToken = request.getHeader("X-Confirm-Token");
        String operation = sensitiveOp.value();
        if (operation.isEmpty()) {
            MethodSignature signature = (MethodSignature) joinPoint.getSignature();
            operation = signature.getDeclaringTypeName() + "." + signature.getName();
        }

        if (!sensitiveOperationService.validateAndConsumeConfirmToken(userId, operation, confirmToken)) {
            throw new BusinessException(403, "敏感操作需二次确认，请先获取确认令牌");
        }
    }
}

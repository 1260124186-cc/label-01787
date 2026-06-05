package com.xiaoan.bookstore.aspect;

import com.xiaoan.bookstore.annotation.Log;
import com.xiaoan.bookstore.common.Constants;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.entity.OperationLog;
import com.xiaoan.bookstore.mapper.OperationLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

@Aspect
@Component
@RequiredArgsConstructor
public class OperationLogAspect {

    private static final Logger log = LoggerFactory.getLogger(OperationLogAspect.class);
    private final OperationLogMapper operationLogMapper;

    @Around("@annotation(logAnno)")
    public Object around(ProceedingJoinPoint point, Log logAnno) throws Throwable {
        Object result = point.proceed();
        try {
            saveLog(point, logAnno);
        } catch (Exception e) {
            log.error("保存操作日志失败", e);
        }
        return result;
    }

    private void saveLog(ProceedingJoinPoint point, Log logAnno) {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return;

        HttpServletRequest request = attrs.getRequest();
        OperationLog opLog = new OperationLog();

        Long tenantId = TenantContext.getTenantId();
        Long roleId = TenantContext.getRoleId();
        if (tenantId != null) opLog.setUserId(tenantId);
        if (TenantContext.getUserType() != null) opLog.setUserType(TenantContext.getUserType());

        opLog.setAction(logAnno.value());
        MethodSignature signature = (MethodSignature) point.getSignature();
        opLog.setTarget(signature.getDeclaringTypeName() + "." + signature.getName());

        StringBuilder detail = new StringBuilder();
        if (roleId != null) {
            detail.append("roleId=").append(roleId);
        }
        opLog.setDetail(detail.length() > 0 ? detail.toString() : null);

        opLog.setIp(getClientIp(request));
        opLog.setCreatedAt(LocalDateTime.now());

        operationLogMapper.insert(opLog);
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // X-Forwarded-For 可能包含多个IP，取第一个
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        // IPv6 本地回环地址转为 IPv4
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}

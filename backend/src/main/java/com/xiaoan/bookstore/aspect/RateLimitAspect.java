package com.xiaoan.bookstore.aspect;

import com.xiaoan.bookstore.annotation.RateLimit;
import com.xiaoan.bookstore.common.TenantContext;
import com.xiaoan.bookstore.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@RequiredArgsConstructor
public class RateLimitAspect {

    private static final Logger log = LoggerFactory.getLogger(RateLimitAspect.class);
    private static final String RATE_LIMIT_PREFIX = "rate_limit:";

    private final RedisTemplate<String, String> redisTemplate;

    @Around("@annotation(rateLimit)")
    public Object around(ProceedingJoinPoint point, RateLimit rateLimit) throws Throwable {
        String key = buildKey(point, rateLimit);
        int limit = rateLimit.limit();
        int windowSeconds = rateLimit.windowSeconds();

        try {
            Long currentCount = redisTemplate.opsForValue().increment(key, 1);
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }

            if (currentCount != null && currentCount > limit) {
                log.warn("接口限流触发: key={}, count={}, limit={}", key, currentCount, limit);
                throw new BusinessException(429, "请求过于频繁，请稍后再试");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Redis限流检查失败，跳过限流: {}", e.getMessage());
        }

        return point.proceed();
    }

    private String buildKey(ProceedingJoinPoint point, RateLimit rateLimit) {
        MethodSignature signature = (MethodSignature) point.getSignature();
        String methodName = signature.getDeclaringTypeName() + "." + signature.getName();
        String keySuffix;

        switch (rateLimit.type()) {
            case IP:
                keySuffix = "ip:" + getClientIp();
                break;
            case USER:
                Long userId = TenantContext.getTenantId();
                if (userId != null) {
                    keySuffix = "user:" + userId;
                } else {
                    keySuffix = "ip:" + getClientIp();
                }
                break;
            case CUSTOM:
            default:
                keySuffix = rateLimit.key();
                break;
        }

        return RATE_LIMIT_PREFIX + methodName + ":" + keySuffix;
    }

    private String getClientIp() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) return "unknown";

        HttpServletRequest request = attrs.getRequest();
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        if (ip != null && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        if ("0:0:0:0:0:0:0:1".equals(ip) || "::1".equals(ip)) {
            ip = "127.0.0.1";
        }
        return ip;
    }
}

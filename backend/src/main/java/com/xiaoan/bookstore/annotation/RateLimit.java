package com.xiaoan.bookstore.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RateLimit {
    String key() default "";
    int limit() default 10;
    int windowSeconds() default 60;
    RateLimitType type() default RateLimitType.USER;

    enum RateLimitType {
        IP,
        USER,
        CUSTOM
    }
}

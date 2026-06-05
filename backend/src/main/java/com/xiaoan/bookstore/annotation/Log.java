package com.xiaoan.bookstore.annotation;

import java.lang.annotation.*;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Log {
    /** 操作描述 */
    String value() default "";
}

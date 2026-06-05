package com.xiaoan.bookstore.aspect;

import com.xiaoan.bookstore.annotation.TenantIgnore;
import com.xiaoan.bookstore.common.TenantContext;
import org.aspectj.lang.annotation.After;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class TenantIgnoreAspect {

    @Before("@annotation(tenantIgnore) || @within(tenantIgnore)")
    public void beforeIgnore(TenantIgnore tenantIgnore) {
        TenantContext.setIgnoreTenant(true);
    }

    @After("@annotation(tenantIgnore) || @within(tenantIgnore)")
    public void afterIgnore(TenantIgnore tenantIgnore) {
        TenantContext.setIgnoreTenant(false);
    }
}

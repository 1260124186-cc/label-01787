package com.xiaoan.bookstore.common;

public class TenantContext {

    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<Integer> USER_TYPE = new ThreadLocal<>();
    private static final ThreadLocal<Long> ROLE_ID = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> IGNORE_TENANT = new ThreadLocal<>();

    public static void set(Long tenantId, Integer userType, Long roleId) {
        TENANT_ID.set(tenantId);
        USER_TYPE.set(userType);
        ROLE_ID.set(roleId);
    }

    public static Long getTenantId() {
        return TENANT_ID.get();
    }

    public static Integer getUserType() {
        return USER_TYPE.get();
    }

    public static Long getRoleId() {
        return ROLE_ID.get();
    }

    public static void setIgnoreTenant(boolean ignore) {
        IGNORE_TENANT.set(ignore);
    }

    public static boolean isIgnoreTenant() {
        Boolean val = IGNORE_TENANT.get();
        return val != null && val;
    }

    public static boolean isMpUser() {
        Integer ut = USER_TYPE.get();
        return ut != null && ut == Constants.USER_TYPE_MP;
    }

    public static boolean isAdminUser() {
        Integer ut = USER_TYPE.get();
        return ut != null && ut == Constants.USER_TYPE_ADMIN;
    }

    public static void clear() {
        TENANT_ID.remove();
        USER_TYPE.remove();
        ROLE_ID.remove();
        IGNORE_TENANT.remove();
    }
}
